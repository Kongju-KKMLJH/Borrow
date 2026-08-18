package kkmljh.borrow.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.ai.dto.AnalyzeRequest;
import kkmljh.borrow.ai.dto.MatchRequest;
import kkmljh.borrow.ai.dto.RequirementResponse;
import kkmljh.borrow.ai.dto.SpaceMatchResult;
import kkmljh.borrow.ai.service.ActivityAnalysisService;
import kkmljh.borrow.ai.service.SpaceMatchingService;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 공간 매칭 (A 담당): A-01 활동 분석, A-02/A-03 매칭, A-04 대체 추천. */
@Tag(name = "AI 매칭", description = "활동 분석 및 공간 적합도 매칭")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ActivityAnalysisService analysisService;
    private final SpaceMatchingService matchingService;

    @Operation(summary = "A-01 활동 분석", description = "활동 설명 자유 텍스트 → 구조화된 공간 요구조건")
    @PostMapping("/analyze")
    public ApiResponse<RequirementResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        return ApiResponse.ok(analysisService.analyze(request));
    }

    @Operation(summary = "A-02/A-03 공간 매칭",
            description = "하드 필터 후 적합도 점수 순으로 공간 추천(matched)과 개최 전 주의사항(cautions)을 준다. "
                    + "excludeSpaceIds를 채우면 A-04 대체 추천. "
                    + "조건에 맞는 공간이 없으면 오류가 아니라 200 + matched=[] + suggestions(조건 수정 안내)로 응답한다.")
    @PostMapping("/match")
    public ApiResponse<SpaceMatchResult> match(@Valid @RequestBody MatchRequest request) {
        return ApiResponse.ok(matchingService.match(request));
    }
}