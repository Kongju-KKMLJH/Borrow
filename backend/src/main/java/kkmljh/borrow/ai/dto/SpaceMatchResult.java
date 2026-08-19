package kkmljh.borrow.ai.dto;

import java.util.List;

/**
 * A-02/A-03 매칭 결과 전체 (기능명세 3.1 {@code display} · 3.1.1 {@code exceptions}).
 *
 * <p><b>후보 0건은 오류가 아니라 정상 결과다.</b> {@code NO_MATCHING_SPACE}(404)로 만들면
 * 프론트가 에러 분기를 따로 짜야 하고, "조건을 어떻게 고치면 되는지"를 실을 자리도 없어진다.
 * 그래서 200으로 {@code matched: []} + {@code suggestions}(조건 수정 안내)를 내려준다.
 *
 * @param matched     적합도 점수 내림차순 추천 목록. 후보가 없으면 빈 목록
 * @param suggestions 조건 수정 안내. <b>{@code matched}가 비었을 때만</b> 채운다
 */
public record SpaceMatchResult(
        List<SpaceMatchResponse> matched,
        List<String> suggestions
) {
    public static SpaceMatchResult matched(List<SpaceMatchResponse> matched) {
        return new SpaceMatchResult(matched, List.of());
    }

    public static SpaceMatchResult noMatch(List<String> suggestions) {
        return new SpaceMatchResult(List.of(), suggestions);
    }
}
