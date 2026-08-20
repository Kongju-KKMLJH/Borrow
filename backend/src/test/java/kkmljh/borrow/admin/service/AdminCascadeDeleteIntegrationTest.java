package kkmljh.borrow.admin.service;

import jakarta.persistence.EntityManager;
import kkmljh.borrow.activity.repository.ActivityRepository;
import kkmljh.borrow.activity.repository.ParticipationRepository;
import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.auth.repository.ArtistVerificationRepository;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.Participation;
import kkmljh.borrow.domain.Role;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.space.repository.HostingRequestRepository;
import kkmljh.borrow.space.repository.SpaceRepository;
import kkmljh.borrow.space.repository.SpaceSlotRepository;
import kkmljh.borrow.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 연쇄 하드 삭제를 <b>실제 DB(H2)</b>로 검증한다 (기능명세 7.1.2 · 7.3.2).
 *
 * <p><b>Mockito 단위 테스트로는 이 버그를 못 잡는다.</b> {@code AdminCascadeDeleter} 가 지키는 것은
 * "자식을 부모보다 먼저 지운다"는 <b>SQL 실행 순서</b>이고, 리포지토리를 모킹하면 FK 제약이 없어
 * 순서를 뒤집어도 통과한다. 그래서 진짜 스키마 위에서 돌린다.
 *
 * <p>{@code @IntegrationTest} 는 테스트마다 트랜잭션을 롤백하므로, 서비스가 큐에 넣은 삭제를
 * 실제 SQL 로 내보내려면 검증 전에 {@link EntityManager#flush()} 가 필요하다.
 * {@code clear()} 까지 해야 영속성 컨텍스트에 남은 인스턴스가 아니라 DB 를 다시 읽는다.
 */
@IntegrationTest
@DisplayName("AdminCascadeDeleter — 실제 DB 연쇄 삭제 (기능명세 7.1.2 · 7.3.2)")
class AdminCascadeDeleteIntegrationTest {

    @Autowired private AdminUserService adminUserService;
    @Autowired private AdminSpaceService adminSpaceService;

    @Autowired private AppUserRepository userRepository;
    @Autowired private ActivityRepository activityRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private SpaceSlotRepository spaceSlotRepository;
    @Autowired private ParticipationRepository participationRepository;
    @Autowired private HostingRequestRepository hostingRequestRepository;
    @Autowired private ArtistVerificationRepository verificationRepository;
    @Autowired private EntityManager em;

    /** 삭제 대상 회원 — 프로그램도 열고 공간도 등록한, 딸린 데이터가 가장 많은 경우다. */
    private AppUser target;
    /** 삭제 대상의 공간에서 개최하기로 했던 남의 프로그램 — 개최지를 잃고 REJECTED 로 돌아가야 한다. */
    private Activity foreignActivity;
    private Activity myActivity;
    private Space mySpace;
    private Space foreignSpace;
    private SpaceSlot mySlot;
    private Participation participationOnMyActivity;
    private Participation myParticipation;
    private HostingRequest outgoingRequest;
    private HostingRequest incomingRequest;
    private ArtistVerification verification;

    private AppUser user(String loginId, Role role) {
        return userRepository.save(AppUser.builder()
                .loginId(loginId).password("$2a$10$hashed").nickname(loginId).role(role).build());
    }

    private Activity activity(String guestId, String title) {
        return activityRepository.save(Activity.builder()
                .guestId(guestId)
                .hostNickname(guestId)
                .hostCertified(true)
                .type(ActivityType.CLASS)
                .field(ActivityField.ART)
                .title(title)
                .description("설명")
                .date(LocalDate.of(2026, 12, 12))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .capacity(8)
                .entryFee(10_000)
                .build());
    }

    private Space space(String ownerId, String name) {
        return spaceRepository.save(Space.builder()
                .ownerId(ownerId)
                .name(name)
                .region("천안시 서북구 불당동")
                .address("불당대로 1")
                .capacity(20)
                .hourlyFee(10_000)
                .build());
    }

    private Participation participation(Activity activity, String guestId) {
        return participationRepository.save(Participation.builder()
                .activity(activity).guestId(guestId).nickname(guestId).headcount(2).build());
    }

    /**
     * 삭제 대상이 온갖 데이터에 얽혀 있는 상태를 만든다.
     *
     * <pre>
     * target  ─ 개설 ─→ myActivity ─ 참여 ─ citizen
     *         │                     └ 개최요청 →  foreignSpace(host 소유)
     *         ├ 등록 ─→ mySpace ─ 슬롯
     *         │            ↑ 개최요청(승인) ─ foreignActivity(other 소유, PUBLISHED)
     *         ├ 참여 ─→ foreignActivity
     *         └ 예술가 인증 신청
     * </pre>
     */
    @BeforeEach
    void setUp() {
        target = user("target", Role.ARTIST);
        AppUser other = user("other", Role.ARTIST);
        user("citizen", Role.MEMBER);
        user("host", Role.HOST);

        mySpace = space("target", "대상의 스튜디오");
        foreignSpace = space("host", "남의 스튜디오");
        mySlot = spaceSlotRepository.save(SpaceSlot.builder()
                .space(mySpace).dayOfWeek(DayOfWeek.SATURDAY)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(22, 0)).build());

        // 대상이 개설한 프로그램 — 남의 참여 신청과 남의 공간으로 보낸 개최 요청을 달고 있다.
        myActivity = activity("target", "대상의 클래스");
        participationOnMyActivity = participation(myActivity, "citizen");
        outgoingRequest = hostingRequestRepository.save(
                HostingRequest.builder().activity(myActivity).space(foreignSpace).build());
        myActivity.markPending();

        // 남의 프로그램이 대상의 공간에서 개최하기로 확정되고 공개까지 된 상태.
        foreignActivity = activity(other.getLoginId(), "남의 클래스");
        foreignActivity.markPending();
        incomingRequest = hostingRequestRepository.save(
                HostingRequest.builder().activity(foreignActivity).space(mySpace).build());
        incomingRequest.approve();   // → MATCHED
        foreignActivity.publish();   // → PUBLISHED (기능명세 3.3)

        myParticipation = participation(foreignActivity, "target");
        verification = verificationRepository.save(ArtistVerification.builder()
                .loginId("target")
                .portfolioUrl("https://portfolio.example.com/target")
                .career("수채화 클래스 3년")
                .build());

        em.flush();
        em.clear();
    }

    /** 큐에 쌓인 삭제를 SQL 로 내보내고, 검증이 DB 를 다시 읽게 만든다. */
    private void syncWithDatabase() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("회원을 지우면 개설 프로그램·등록 공간·슬롯·참여·개최요청·인증 신청까지 전부 사라진다")
    void deleteUserRemovesEveryDependentRow() {
        adminUserService.delete(target.getId());
        syncWithDatabase();

        assertThat(userRepository.findByLoginId("target")).isEmpty();
        assertThat(activityRepository.findById(myActivity.getId())).isEmpty();
        assertThat(spaceRepository.findById(mySpace.getId())).isEmpty();
        assertThat(spaceSlotRepository.findById(mySlot.getId())).isEmpty();
        assertThat(verificationRepository.findByLoginId("target")).isEmpty();

        assertThat(participationRepository.findById(participationOnMyActivity.getId()))
                .as("삭제된 프로그램에 달린 남의 참여 신청도 함께 지운다")
                .isEmpty();
        assertThat(participationRepository.findById(myParticipation.getId()))
                .as("대상이 남의 프로그램에 낸 참여 신청도 지운다")
                .isEmpty();
        assertThat(hostingRequestRepository.findById(outgoingRequest.getId())).isEmpty();
        assertThat(hostingRequestRepository.findById(incomingRequest.getId())).isEmpty();
    }

    @Test
    @DisplayName("개최지를 잃은 남의 프로그램은 REJECTED 로 되돌아가 다른 공간에 재요청할 수 있다")
    void foreignActivitySurvivesAsRejected() {
        adminUserService.delete(target.getId());
        syncWithDatabase();

        assertThat(activityRepository.findById(foreignActivity.getId()))
                .get()
                .extracting(Activity::getStatus)
                .isEqualTo(ActivityStatus.REJECTED);
    }

    @Test
    @DisplayName("남의 계정·공간은 건드리지 않는다 — 삭제 범위는 대상 회원의 데이터뿐이다")
    void unrelatedRowsRemain() {
        adminUserService.delete(target.getId());
        syncWithDatabase();

        assertThat(userRepository.findByLoginId("other")).isPresent();
        assertThat(userRepository.findByLoginId("citizen")).isPresent();
        assertThat(userRepository.findByLoginId("host")).isPresent();
        assertThat(spaceRepository.findById(foreignSpace.getId()))
                .as("대상이 개최 요청을 보냈을 뿐인 남의 공간은 남는다")
                .isPresent();
    }

    @Test
    @DisplayName("공간만 지우면 프로그램은 살아남고 그 참여 신청도 남는다 (7.3.2)")
    void deleteSpaceKeepsActivityAndItsParticipations() {
        adminSpaceService.delete(mySpace.getId());
        syncWithDatabase();

        assertThat(spaceRepository.findById(mySpace.getId())).isEmpty();
        assertThat(spaceSlotRepository.findById(mySlot.getId())).isEmpty();
        assertThat(hostingRequestRepository.findById(incomingRequest.getId())).isEmpty();

        assertThat(activityRepository.findById(foreignActivity.getId()))
                .get()
                .extracting(Activity::getStatus)
                .isEqualTo(ActivityStatus.REJECTED);
        assertThat(participationRepository.findById(myParticipation.getId()))
                .as("프로그램이 살아 있으므로 그 참여 신청까지 날리지 않는다")
                .isPresent();
    }
}
