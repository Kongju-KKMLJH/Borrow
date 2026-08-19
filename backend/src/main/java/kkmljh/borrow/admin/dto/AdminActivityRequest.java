package kkmljh.borrow.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 임시(mock) 프로그램 생성·수정 요청 (기능명세 7.2.2 dataSpec —
 * 프로그램명·담당 예술가·공간·일정·상태).
 *
 * <p><b>날짜에 {@code @FutureOrPresent} 를 걸지 않는다.</b> 시연용 임시 데이터는 "이미 끝난
 * 프로그램" 같은 과거 상태도 만들어 둬야 화면(마감·종료)을 확인할 수 있다.
 *
 * @param hostLoginId 담당 예술가의 로그인 아이디. 유형(HOBBY/CLASS)과 인증 배지는
 *                    이 계정의 역할에서 서버가 정한다 — 클라이언트가 배지를 위조하지 못하게 하는
 *                    기존 규칙을 관리자 경로에서도 깨지 않는다.
 * @param spaceId     개최지. {@code null} 이면 공간 미확정 상태로 만든다.
 *                    MATCHED·PUBLISHED 상태를 요청하려면 반드시 있어야 한다.
 * @param status      목표 상태. 전이는 기존 도메인 메서드로만 밟는다.
 */
public record AdminActivityRequest(
        @NotBlank(message = "담당 예술가 아이디는 필수입니다.")
        String hostLoginId,

        Long spaceId,

        @NotNull(message = "분야는 필수입니다.")
        ActivityField field,

        @NotBlank(message = "프로그램명은 필수입니다.")
        String title,

        String description,

        List<String> imageUrls,

        @NotNull(message = "날짜는 필수입니다.")
        LocalDate date,

        @NotNull(message = "시작 시각은 필수입니다.")
        LocalTime startTime,

        @NotNull(message = "종료 시각은 필수입니다.")
        LocalTime endTime,

        @Min(value = 1, message = "모집 정원은 1명 이상이어야 합니다.")
        int capacity,

        @Min(value = 0, message = "참가비는 0원 이상이어야 합니다.")
        int entryFee,

        @NotNull(message = "상태는 필수입니다.")
        ActivityStatus status
) {
    public List<String> imageUrlsOrEmpty() {
        return imageUrls == null ? List.of() : imageUrls;
    }

    /** 개최지가 확정돼야만 도달할 수 있는 상태인지 (기존 상태 흐름 그대로) */
    public boolean requiresSpace() {
        return status == ActivityStatus.MATCHED || status == ActivityStatus.PUBLISHED;
    }
}
