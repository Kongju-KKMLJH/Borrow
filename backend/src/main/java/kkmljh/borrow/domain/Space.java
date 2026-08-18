package kkmljh.borrow.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 유휴 공간 (B-02 기본정보, B-03 시설, B-04 허용활동, B-06 이용조건) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유자(공간 제공자)의 로그인 아이디 — 수정·삭제·요청 처리 권한 판정 기준 */
    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private String name;

    /** 시/구/동 단위 텍스트 지역 (예: "천안시 서북구 불당동") — 매칭 필터에 사용 */
    @Column(nullable = false)
    private String region;

    private String address;

    /** 공간 사진 URL 목록 (앱에서 촬영/선택 후 업로드하여 받은 상대 URL들, 순서 보존) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "space_image", joinColumns = @JoinColumn(name = "space_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url", length = 1000)
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false)
    private int capacity;

    /** 시간당 이용료(원) */
    @Column(nullable = false)
    private int hourlyFee;

    /** 이용 조건 자유 텍스트 (음료 주문, 정리 시간 등, B-06) */
    @Column(length = 1000)
    private String conditions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "space_facility", joinColumns = @JoinColumn(name = "space_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "facility")
    private Set<FacilityType> facilities = new HashSet<>();

    /** 허용 활동 분야 (B-04) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "space_allowed_field", joinColumns = @JoinColumn(name = "space_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "field")
    private Set<ActivityField> allowedFields = new HashSet<>();

    /** 소음 발생 활동 허용 여부 (B-04 제한 조건) */
    private boolean noiseAllowed;

    /** 오염(물감 등) 발생 활동 허용 여부 (B-04 제한 조건) */
    private boolean messAllowed;

    @Builder
    private Space(String ownerId, String name, String region, String address, List<String> imageUrls,
                  int capacity, int hourlyFee, String conditions,
                  Set<FacilityType> facilities, Set<ActivityField> allowedFields,
                  boolean noiseAllowed, boolean messAllowed) {
        this.ownerId = ownerId;
        this.name = name;
        this.region = region;
        this.address = address;
        // 방어적 복사 — 호출자가 넘긴 컬렉션을 그대로 들면 ⑴ List.of() 같은 불변 컬렉션일 때
        // update* 의 clear() 가 터지고 ⑵ 호출자가 원본을 바꾸면 엔티티 상태가 몰래 따라 바뀐다.
        if (imageUrls != null) this.imageUrls = new ArrayList<>(imageUrls);
        this.capacity = capacity;
        this.hourlyFee = hourlyFee;
        this.conditions = conditions;
        if (facilities != null) this.facilities = new HashSet<>(facilities);
        if (allowedFields != null) this.allowedFields = new HashSet<>(allowedFields);
        this.noiseAllowed = noiseAllowed;
        this.messAllowed = messAllowed;
    }

    /** 이 공간의 소유자(공간 제공자)인지 */
    public boolean isOwnedBy(String loginId) {
        return this.ownerId.equals(loginId);
    }

    public void updateBasicInfo(String name, String region, String address, List<String> imageUrls, int capacity) {
        this.name = name;
        this.region = region;
        this.address = address;
        this.imageUrls.clear();
        if (imageUrls != null) this.imageUrls.addAll(imageUrls);
        this.capacity = capacity;
    }

    public void updateFacilities(Set<FacilityType> facilities) {
        this.facilities.clear();
        this.facilities.addAll(facilities);
    }

    public void updateAllowedActivities(Set<ActivityField> allowedFields, boolean noiseAllowed, boolean messAllowed) {
        this.allowedFields.clear();
        this.allowedFields.addAll(allowedFields);
        this.noiseAllowed = noiseAllowed;
        this.messAllowed = messAllowed;
    }

    public void updateFeeAndConditions(int hourlyFee, String conditions) {
        this.hourlyFee = hourlyFee;
        this.conditions = conditions;
    }
}
