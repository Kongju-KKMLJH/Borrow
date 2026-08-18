package kkmljh.borrow.space.dto;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceRequirement;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** 개최 요청 응답 (B-07 목록, B-08 상세) — 사업자가 승인/거절 판단에 필요한 정보 (기능명세 3.2 display) */
public record HostingRequestResponse(
        Long id,
        RequestStatus status,
        String rejectReason,
        SpaceInfo space,
        ActivityInfo activity,
        /** 활동 일정이 공간에 등록된 유휴시간(슬롯) 어디에도 완전히 포함되지 않으면 true (F-XOKOSU) */
        boolean scheduleMismatch
) {
    /**
     * 승인 판단에 필요한 공간 정보 (기능명세 3.2 {@code display} — "공간 이용료를 표시한다").
     *
     * <p>⚠️ <b>{@code address} 를 넣지 마라.</b> 공개 응답의 주소는 동 단위({@code region})까지라는
     * 기능명세 6.1 {@code rules} 정책(이슈 #53)이 여기서 뚫린다. 이용료는 이미 공개 공간 응답
     * ({@code SpaceResponse.hourlyFee})에 있는 값이라 노출 정책과 무관하다.
     *
     * @param hourlyFee         시간당 단가(원)
     * @param expectedRentalFee 이 요청의 활동 시간 기준 예상 이용료(원). 예술가가 매칭 확정 화면에서 보는
     *                          {@code PriceBreakdown.spaceRentalFee} 와 <b>같은 값</b>이다 (계산: {@link Space#rentalFeeFor})
     */
    public record SpaceInfo(Long id, String name, String region, int hourlyFee, int expectedRentalFee) {
        static SpaceInfo from(Space space, Activity activity) {
            return new SpaceInfo(
                    space.getId(),
                    space.getName(),
                    space.getRegion(),
                    space.getHourlyFee(),
                    space.rentalFeeFor(activity.getStartTime(), activity.getEndTime()));
        }
    }

    public record ActivityInfo(
            Long id,
            String title,
            String description,
            ActivityField field,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int capacity,
            int entryFee,
            String hostNickname,
            RequirementInfo requirement
    ) {
        static ActivityInfo from(Activity activity) {
            return new ActivityInfo(
                    activity.getId(),
                    activity.getTitle(),
                    activity.getDescription(),
                    activity.getField(),
                    activity.getDate(),
                    activity.getStartTime(),
                    activity.getEndTime(),
                    activity.getCapacity(),
                    activity.getEntryFee(),
                    activity.getHostNickname(),
                    RequirementInfo.from(activity.getRequirement())
            );
        }
    }

    /** 활동의 공간 요구조건 (B-08: 인원, 시설 사용 내용, 소음·오염 여부) */
    public record RequirementInfo(
            Integer headcount,
            Set<FacilityType> requiredFacilities,
            Boolean noisy,
            Boolean messy
    ) {
        static RequirementInfo from(SpaceRequirement requirement) {
            // 요구조건 없이 개설한 활동은 값이 전부 null인 인스턴스로 되살아난다 → null로 정규화
            if (requirement == null || requirement.isEmpty()) {
                return null;
            }
            return new RequirementInfo(
                    requirement.getHeadcount(),
                    requirement.getRequiredFacilities(),
                    requirement.getNoisy(),
                    requirement.getMessy()
            );
        }
    }

    public static HostingRequestResponse from(HostingRequest request, boolean scheduleMismatch) {
        return new HostingRequestResponse(
                request.getId(),
                request.getStatus(),
                request.getRejectReason(),
                SpaceInfo.from(request.getSpace(), request.getActivity()),
                ActivityInfo.from(request.getActivity()),
                scheduleMismatch
        );
    }
}
