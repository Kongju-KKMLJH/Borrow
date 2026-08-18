package kkmljh.borrow.space.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;

import java.util.List;
import java.util.Set;

/** 공간 등록/수정 요청 (B-02 기본정보, B-03 시설, B-04 허용활동, B-06 이용조건) */
public record SpaceRequest(
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

        boolean messAllowed
) {
    /**
     * 중복 판정·저장에 함께 쓰는 정규화 값 (기능명세 6.1 exceptions).
     * 정규화는 trim()까지만 한다 — 공백 접기·대소문자 정규화는 한글 주소에 효과가 거의 없는데
     * "왜 이건 중복이 아니냐"는 경계 질문만 늘린다.
     */
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
}
