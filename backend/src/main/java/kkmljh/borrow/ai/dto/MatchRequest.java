package kkmljh.borrow.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * A-02/A-03 공간 매칭 요청. A-01 결과 + 활동 일시로 구성한다.
 * {@code excludeSpaceIds}가 채워지면 A-04(대체 추천) — 거절된 공간을 제외하고 재매칭.
 *
 * @param region             희망 지역 (부분 일치 필터, 비우면 지역 무관)
 * @param headcount          필요 수용 인원 (하드 필터)
 * @param requiredFacilities 필요 시설 (적합도 점수에 반영, 하드 필터는 아님)
 * @param noisy              소음 발생 활동 여부 (소음 불가 공간 제외)
 * @param messy              오염 발생 활동 여부 (오염 불가 공간 제외)
 * @param field              활동 분야 (허용 분야 하드 필터)
 * @param date               활동 날짜 (요일 → 슬롯 시간 겹침 판정)
 * @param startTime          시작 시각
 * @param endTime            종료 시각
 * @param excludeSpaceIds    제외할 공간 ID (A-04 대체 추천용, 선택)
 */
public record MatchRequest(
        String region,
        @Positive(message = "수용 인원은 1명 이상이어야 합니다.")
        int headcount,
        List<FacilityType> requiredFacilities,
        Boolean noisy,
        Boolean messy,
        @NotNull(message = "활동 분야는 필수입니다.")
        ActivityField field,
        @NotNull(message = "활동 날짜는 필수입니다.")
        LocalDate date,
        @NotNull(message = "시작 시각은 필수입니다.")
        LocalTime startTime,
        @NotNull(message = "종료 시각은 필수입니다.")
        LocalTime endTime,
        List<Long> excludeSpaceIds
) {
    public boolean noisyOrFalse() {
        return Boolean.TRUE.equals(noisy);
    }

    public boolean messyOrFalse() {
        return Boolean.TRUE.equals(messy);
    }

    public String regionOrEmpty() {
        return region == null ? "" : region.trim();
    }

    public List<FacilityType> requiredFacilitiesOrEmpty() {
        return requiredFacilities == null ? List.of() : requiredFacilities;
    }

    public List<Long> excludeSpaceIdsOrEmpty() {
        return excludeSpaceIds == null ? List.of() : excludeSpaceIds;
    }
}