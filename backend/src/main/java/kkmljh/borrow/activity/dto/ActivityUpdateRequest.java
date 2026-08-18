package kkmljh.borrow.activity.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kkmljh.borrow.domain.ActivityField;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 활동 수정 (기능명세 2.1) — 전체 교체(PUT).
 *
 * <p>type·hostCertified·hostNickname 은 받지 않는다 — 서버가 로그인 역할·계정에서 정한다
 * (클라이언트가 보내면 CLASS 배지나 남의 이름을 위조할 수 있다). guestId·status 도 마찬가지로
 * 소유자·상태는 다른 경로로만 바뀐다.
 *
 * <p>공간 요구조건은 이 API 의 대상이 아니다 — {@code PATCH /api/activities/{id}/requirement}(U-08)
 * 가 담당한다. 진입점이 둘이 되면 어느 쪽이 최종값인지 갈린다.
 */
public record ActivityUpdateRequest(
        @NotNull ActivityField field,
        @NotBlank String title,
        String description,
        List<String> imageUrls,
        @NotNull @FutureOrPresent LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Positive int capacity,
        @Min(0) int entryFee
) {
}
