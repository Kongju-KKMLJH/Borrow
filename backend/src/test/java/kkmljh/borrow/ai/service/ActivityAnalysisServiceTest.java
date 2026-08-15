package kkmljh.borrow.ai.service;

import com.anthropic.client.AnthropicClient;
import kkmljh.borrow.ai.dto.AnalyzeRequest;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * A-01 활동 분석. Claude 성공 파싱 경로는 SDK {@code StructuredMessage} 구성이 필요해 통합테스트로 남기고,
 * 여기서는 호출 실패 시 {@code AI_ANALYSIS_FAILED}로 변환되는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityAnalysisService — A-01 실패 처리")
class ActivityAnalysisServiceTest {

    @Mock
    private AnthropicClient anthropic;

    @InjectMocks
    private ActivityAnalysisService service;

    @Test
    @DisplayName("Claude 호출이 실패하면 AI_ANALYSIS_FAILED로 변환한다")
    void wrapsFailureAsBusinessException() {
        when(anthropic.messages()).thenThrow(new RuntimeException("offline"));

        assertThatThrownBy(() -> service.analyze(new AnalyzeRequest("수채화 드로잉 모임", "천안시 서북구")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_ANALYSIS_FAILED);
    }
}
