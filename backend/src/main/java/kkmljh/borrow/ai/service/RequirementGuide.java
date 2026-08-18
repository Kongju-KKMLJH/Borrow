package kkmljh.borrow.ai.service;

import kkmljh.borrow.domain.FacilityType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기능명세 2.2 · 2.2.1 — 추출된 운영 조건에서 <b>빠진 항목</b>을 짚고 보완 질문을 만든다.
 *
 * <p><b>LLM을 다시 부르지 않는다.</b> "무엇이 비었는가"는 서버가 확정적으로 판정할 수 있고,
 * 추출과 진단을 한 호출에 섞으면 추출 품질이 흔들린다
 * ({@code AnalyzedRequirement} 스키마에 질문 필드를 넣지 마라).
 *
 * <p>질문 문구는 여기 한 곳에만 둔다. <b>판정 근거가 없는 항목은 묻지 않는다</b> —
 * 예를 들어 소요 시간은 추출 스키마에 없어 비었는지 알 수 없고, 답을 받아도 채울 자리가 없다.
 */
public final class RequirementGuide {

    public static final String REGION = "region";
    public static final String HEADCOUNT = "headcount";
    public static final String REQUIRED_FACILITIES = "requiredFacilities";

    /** 필드 → 보완 질문. 순서를 보존해 화면에 늘 같은 순서로 나가게 한다. */
    private static final Map<String, String> QUESTIONS = new LinkedHashMap<>();

    static {
        QUESTIONS.put(REGION, "어느 지역에서 열고 싶으신가요? (예: 천안시 서북구 불당동)");
        QUESTIONS.put(HEADCOUNT, "몇 명이 참여할 예정인가요? 주최자를 포함한 예상 인원을 알려 주세요.");
        QUESTIONS.put(REQUIRED_FACILITIES, "공간에 꼭 필요한 시설이 있나요? (예: 수도, 콘센트, 조명, 테이블)");
    }

    private RequirementGuide() {
    }

    /**
     * 비어 있어 정확한 추천이 어려운 항목들. 판정은 <b>추출 결과의 빈 값</b>으로 끝낸다.
     *
     * @param headcount <b>보정 전</b> 원본 추출값. 응답에 담기는 값은 1 이상으로 보정되므로
     *                  보정 후 값으로 판정하면 "빠졌다"를 영영 알아채지 못한다.
     */
    public static List<String> missingFields(String region, int headcount, List<FacilityType> requiredFacilities) {
        List<String> missing = new ArrayList<>();
        if (region == null || region.isBlank()) {
            missing.add(REGION);
        }
        if (headcount <= 0) {
            missing.add(HEADCOUNT);
        }
        if (requiredFacilities == null || requiredFacilities.isEmpty()) {
            missing.add(REQUIRED_FACILITIES);
        }
        return List.copyOf(missing);
    }

    /** 빠진 항목에 대응하는 보완 질문. 아는 질문이 없는 항목은 조용히 건너뛴다. */
    public static List<String> questionsFor(List<String> missingFields) {
        return missingFields.stream()
                .map(QUESTIONS::get)
                .filter(question -> question != null)
                .toList();
    }
}
