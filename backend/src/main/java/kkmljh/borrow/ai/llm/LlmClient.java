package kkmljh.borrow.ai.llm;

/**
 * LLM 호출 추상화. A-01/A-03가 provider(Anthropic·OpenAI 호환)에 의존하지 않도록 감싼다.
 *
 * <p>구현체·모델은 {@code ai.provider} / {@code ai.model} 설정으로 서버 기동 시점에 고정된다
 * (AiConfig 참고). 서비스 코드는 이 인터페이스만 본다.
 */
public interface LlmClient {

    /**
     * 프롬프트를 보내고 structured output(JSON 스키마)으로 파싱된 {@code schema} 인스턴스를 돌려준다.
     *
     * @param schema    구조화 출력 스키마 클래스(Jackson 애노테이션으로 필드 설명 부여)
     * @param maxTokens 응답 최대 토큰
     * @return 파싱된 결과. 응답에 구조화 컨텐츠가 없으면 {@code null}
     * @throws RuntimeException 네트워크·API·파싱 예외는 그대로 전파(호출부에서 폴백/에러 처리)
     */
    <T> T complete(String prompt, Class<T> schema, long maxTokens);
}
