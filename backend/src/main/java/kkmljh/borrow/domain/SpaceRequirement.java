package kkmljh.borrow.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * 활동의 공간 요구조건 (U-08 직접 입력 또는 A-01 AI 자동 분석 결과).
 * Activity에 @Embedded로 포함된다.
 *
 * <p>⚠️ 필드는 반드시 <b>래퍼 타입</b>이어야 한다. 요구조건 없이 활동을 만들면 {@code req_*} 컬럼이
 * 전부 NULL로 저장되는데, 이 임베더블은 {@code @ElementCollection}을 품고 있어 Hibernate가
 * "전부 NULL이면 null" 최적화를 적용하지 않고 항상 인스턴스화한다. primitive면 그 시점에
 * "Null value was assigned to a property of primitive type"로 조회가 통째로 실패한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceRequirement {

    /** 희망 지역 (예: "천안시 서북구") */
    @Column(name = "req_region")
    private String region;

    /** 필요 수용 인원 */
    @Column(name = "req_headcount")
    private Integer headcount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "activity_required_facility", joinColumns = @JoinColumn(name = "activity_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "facility")
    private Set<FacilityType> requiredFacilities = new HashSet<>();

    /** 소음이 발생하는 활동인지 */
    @Column(name = "req_noisy")
    private Boolean noisy;

    /** 오염(물감 등)이 발생하는 활동인지 */
    @Column(name = "req_messy")
    private Boolean messy;

    @Builder
    private SpaceRequirement(String region, Integer headcount, Set<FacilityType> requiredFacilities,
                             Boolean noisy, Boolean messy) {
        this.region = region;
        this.headcount = headcount;
        if (requiredFacilities != null) this.requiredFacilities = requiredFacilities;
        this.noisy = noisy;
        this.messy = messy;
    }

    /**
     * 실제로 입력된 요구조건이 하나도 없는지. 요구조건 없이 개설한 활동은 Hibernate가
     * 값이 전부 null인 인스턴스로 되살리므로, 응답에서는 이 경우를 null로 정규화한다
     * (기존 API 계약대로 {@code requirement: null}이 내려가게 유지).
     */
    public boolean isEmpty() {
        return region == null && headcount == null && noisy == null && messy == null
                && (requiredFacilities == null || requiredFacilities.isEmpty());
    }
}
