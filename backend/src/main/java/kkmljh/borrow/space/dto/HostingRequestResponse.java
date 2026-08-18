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

/** 개최 요청 응답 (B-07 목록, B-08 상세) — 사업자가 승인/거절 판단에 필요한 정보 */
public record HostingRequestResponse(
        Long id,
        RequestStatus status,
        String rejectReason,
        SpaceInfo space,
        ActivityInfo activity,
        /** 활동 일정이 공간에 등록된 유휴시간(슬롯) 어디에도 완전히 포함되지 않으면 true (F-XOKOSU) */
        boolean scheduleMismatch
) {
    public record SpaceInfo(Long id, String name, String region) {
        static SpaceInfo from(Space space) {
            return new SpaceInfo(space.getId(), space.getName(), space.getRegion());
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
                SpaceInfo.from(request.getSpace()),
                ActivityInfo.from(request.getActivity()),
                scheduleMismatch
        );
    }
}
