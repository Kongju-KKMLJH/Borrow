package kkmljh.borrow.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.space.dto.SpaceSlotRequest;

import java.util.List;
import java.util.Set;

/**
 * 관리자 콘솔의 공간 생성·수정 요청 (기능명세 7.3.2 dataSpec).
 *
 * <p>{@code SpaceRequest} 를 그대로 쓰지 않는 이유는 두 가지다 — 관리자는 <b>등록자(ownerId)를
 * 지정</b>해야 하고, <b>이용 가능 시간</b>을 함께 만들어야 한다. A-02 매칭이 슬롯 시간 겹침으로
 * 후보를 거르므로, 슬롯 없는 공간은 "AI 추천 후보에 즉시 반영된다"(7.3.2 outcome)를 만족하지 못한다.
 *
 * @param slots 이용 가능 시간. 수정 시에는 <b>전체 교체</b>다(빈 배열이면 전부 삭제).
 */
public record AdminSpaceRequest(
        @NotBlank(message = "등록자(공간 파트너) 아이디는 필수입니다.")
        String ownerId,

        @NotBlank(message = "공간 이름은 필수입니다.")
        String name,

        @NotBlank(message = "지역은 필수입니다.")
        String region,

        String address,

        List<String> imageUrls,

        @Min(value = 1, message = "수용 인원은 1명 이상이어야 합니다.")
        int capacity,

        @Min(value = 0, message = "이용료는 0원 이상이어야 합니다.")
        int hourlyFee,

        String conditions,

        Set<FacilityType> facilities,

        Set<ActivityField> allowedFields,

        boolean noiseAllowed,

        boolean messAllowed,

        @Valid
        List<SpaceSlotRequest> slots
) {
    public String trimmedName() {
        return name == null ? null : name.trim();
    }

    public String trimmedAddress() {
        return address == null ? null : address.trim();
    }

    public List<String> imageUrlsOrEmpty() {
        return imageUrls == null ? List.of() : imageUrls;
    }

    public Set<FacilityType> facilitiesOrEmpty() {
        return facilities == null ? Set.of() : facilities;
    }

    public Set<ActivityField> allowedFieldsOrEmpty() {
        return allowedFields == null ? Set.of() : allowedFields;
    }

    public List<SpaceSlotRequest> slotsOrEmpty() {
        return slots == null ? List.of() : slots;
    }
}
