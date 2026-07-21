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
    private int headcount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "activity_required_facility", joinColumns = @JoinColumn(name = "activity_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "facility")
    private Set<FacilityType> requiredFacilities = new HashSet<>();

    /** 소음이 발생하는 활동인지 */
    @Column(name = "req_noisy")
    private boolean noisy;

    /** 오염(물감 등)이 발생하는 활동인지 */
    @Column(name = "req_messy")
    private boolean messy;

    @Builder
    private SpaceRequirement(String region, int headcount, Set<FacilityType> requiredFacilities,
                             boolean noisy, boolean messy) {
        this.region = region;
        this.headcount = headcount;
        if (requiredFacilities != null) this.requiredFacilities = requiredFacilities;
        this.noisy = noisy;
        this.messy = messy;
    }
}
