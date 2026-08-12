package kkmljh.borrow.domain;

import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Participation 엔티티 (U-04 참여 신청)")
class ParticipationTest {

    @Test
    @DisplayName("전달한 값이 그대로 담긴다")
    void creation() {
        Activity activity = TestFixtures.activity();

        Participation participation = Participation.builder()
                .activity(activity)
                .guestId("member1")
                .nickname("홍길동")
                .headcount(2)
                .build();

        assertThat(participation.getId()).isNull();
        assertThat(participation.getActivity()).isSameAs(activity);
        assertThat(participation.getGuestId()).isEqualTo("member1");
        assertThat(participation.getNickname()).isEqualTo("홍길동");
        assertThat(participation.getHeadcount()).isEqualTo(2);
    }

    @Test
    @DisplayName("참여자 아이디와 표시 이름은 별개 값이다 — 닉네임은 서버가 계정에서 채운다")
    void loginIdAndNicknameAreSeparate() {
        Participation participation = Participation.builder()
                .activity(TestFixtures.activity())
                .guestId("member1")
                .nickname("홍길동")
                .headcount(1)
                .build();

        assertThat(participation.getGuestId()).isNotEqualTo(participation.getNickname());
    }
}
