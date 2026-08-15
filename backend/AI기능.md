# AI 공간 매칭 기능 문서

천안 유휴공간 대여 서비스에서 게스트가 개설하려는 취미 모임(그림·촬영)에 적합한 공간을
LLM으로 추천하는 기능이다. 담당 패키지는 `ai/`(A 담당).

## 개요

매칭은 4단계 파이프라인으로 동작하며, **LLM(Claude) 호출은 A-01과 A-03 두 곳에서만** 일어난다.
나머지 단계(A-02 하드 필터, A-04 재실행)는 순수 DB/자바 로직이다.

```
활동 설명(자유 텍스트)
   │
 [A-01] LLM 호출 ①  ── 설명 → 구조화된 요구조건(분야/인원/시설/소음·오염)
   │
 [A-02] DB + 자바    ── 하드 필터로 후보 공간 추림 (LLM 아님)
   │
 [A-03] LLM 호출 ②  ── 후보들에 0~100점 적합도 + 이유 부여, 점수 내림차순 정렬
   │
 [A-04] 재실행       ── 거절 공간 제외 후 A-02+A-03 다시 (같은 진입점)
```

설계 원칙: **"LLM은 이해(A-01)와 판단(A-03)에만, 필터링(A-02)은 결정적 DB 로직으로."**
A-03에는 규칙 기반 폴백이 항상 붙어 있어 LLM이 죽어도 매칭이 끊기지 않는다(데모 안정성).

## API 엔드포인트

`AiController` — base path `/api/ai`

| 메서드 | 경로 | 단계 | 설명 |
|---|---|---|---|
| POST | `/api/ai/analyze` | A-01 | 활동 설명 자유 텍스트 → 구조화된 공간 요구조건 |
| POST | `/api/ai/match` | A-02/A-03 | 하드 필터 후 적합도 점수 순으로 공간 추천. `excludeSpaceIds`를 채우면 A-04 대체 추천 |

## 단계별 상세

### A-01 · 활동 분석 (`ActivityAnalysisService`) — LLM 호출 ①

자유 텍스트 활동 설명을 **구조화된 공간 요구조건**으로 변환한다.

- `llm.complete(prompt, AnalyzedRequirement.class, 2048)` 한 번 호출.
- 핵심은 **structured output**: 응답 스키마 클래스(`AnalyzedRequirement`)를 그대로 넘기면
  Claude가 JSON 스키마에 맞춰 채운다. 프롬프트로 추출 규칙을 지시:
  - `field`: 그림/드로잉/미술 → `ART`, 사진/영상 촬영 → `PHOTO`
  - `headcount`: 예상 참여 인원. 명시 없으면 활동 성격에 맞게 추정(보통 4~8명)
  - `requiredFacilities`: 활동에 꼭 필요한 시설만 (예: 수채화 → `WATER`, 실내 촬영 → `LIGHTING`, 장비 → `OUTLET`)
  - `noisy`: 소음이 크게 나는 활동인지
  - `messy`: 물감·흙·음식물 등으로 공간이 더러워질 수 있는 활동인지
- `maxTokens=2048`: Gemini 등 thinking 모델이 추론 토큰을 소비해도 JSON이 잘리지 않도록 여유를 둔 값.
- **폴백 없음**: 실패하면 `BusinessException(ErrorCode.AI_ANALYSIS_FAILED)`를 던진다.
  이 단계가 없으면 매칭 자체가 불가하기 때문.

결과(`RequirementResponse`)가 `/api/ai/match`의 입력(`MatchRequest`)이 된다.

### A-02 · 하드 필터 (`SpaceMatchingService.hardFilter`) — LLM 아님

LLM에 보내기 전에 명백히 안 되는 공간을 먼저 쳐낸다. 두 층위로 구성:

**1. DB 쿼리** (`SpaceMatchRepository.findCandidates`) — 굵은 조건:
- `capacity >= headcount` (수용 인원)
- `region` 부분 일치 (양방향 LIKE, 빈 문자열이면 무관)
- `field member of allowedFields` (허용 분야)

> `Space` 엔티티는 B 소유지만 읽기 전용 조회이므로 AI 매칭 전용 리포지토리를 별도로 둔다
> (B의 `SpaceRepository`와 빈 이름 충돌 회피).

**2. 자바 필터** — DB로 표현하기 번거로운 조건:
- `excludeSpaceIds`에 든 공간 제외 (A-04에서 사용)
- 소음/오염 제한: 요청이 `noisy`면 `noiseAllowed`인 공간만, `messy`면 `messAllowed`인 공간만
- **슬롯 시간 겹침**: 활동 날짜의 요일에 `[startTime, endTime]`을 완전히 포함하는 유휴 슬롯이
  있는 공간만 (`SpaceSlot.covers`)

통과 후보는 **최대 12개(`MAX_CANDIDATES`)**로 제한한다(LLM 프롬프트 비대화 방지).
후보가 0개면 LLM 호출을 건너뛰고 빈 목록을 반환한다.

### A-03 · 적합도 점수 (`SpaceMatchingService.score`) — LLM 호출 ②

필터를 통과한 후보들을 **한 번의 Claude 호출**로 채점한다(후보마다 부르지 않고 목록 전체를 한 프롬프트에).

- 후보를 `CandidateView` record로 요약 → Jackson으로 JSON 직렬화해 프롬프트에 삽입.
- `llm.complete(prompt, SpaceScores.class, 4096)` 호출. structured output으로
  `{spaceId, score(0~100), reason(한국어 한 문장)}` 리스트를 받는다.
- 프롬프트가 지시하는 평가 관점:
  - 필요 시설 충족도 (`requiredFacilities` ⊆ 공간 `facilities`)
  - 수용 인원 여유 (너무 크지도 작지도 않게)
  - 시간당 이용료 (저렴할수록 가점)
  - 활동 성격과 공간 `conditions`의 부합
- 받은 점수를 `spaceId → SpaceScore` 맵으로 만들어 각 공간에 매핑. 점수는 `0~100`으로 클램프.
  최종적으로 **점수 내림차순 정렬**해서 반환.
- `maxTokens=4096`: 후보 다수 점수 출력 + thinking 모델 추론 토큰까지 감안한 여유 상한.

**규칙 기반 폴백 (핵심 안정성 장치):**
LLM 호출이 실패하거나(네트워크/API/파싱 예외) 응답이 비면, `tryAiScores`가 예외를 삼키고
빈 맵을 반환한다. 그러면 각 공간은 `ruleScore`/`ruleReason`으로 대체 채점된다:

- 기본 55점에서 시작
- 시설 충족 비율에 따라 최대 +30 (요구 시설 없으면 +15)
- 인원 여유(slack)에 따라 +3 ~ +15 (여유가 적을수록 가점)
- 시간당 요금 구간에 따라 +0 ~ +10 (≤1만원 +10, ≤2만원 +5)
- 100점 상한

응답의 `aiScored` 플래그로 각 항목이 AI 점수인지 규칙 폴백인지 구분한다.
해커톤 데모에서 API 키/네트워크가 죽어도 매칭 결과가 나오도록 하는 설계다.

### A-04 · 대체 추천 — 별도 코드 없음

사용자가 추천 공간을 거절하면, 그 `spaceId`들을 `MatchRequest.excludeSpaceIds`에 담아
`/api/ai/match`를 **다시 호출**한다. A-02 필터에서 제외되고 A-03가 다시 도니,
동일 진입점 재실행만으로 "다음 후보"를 얻는다.

## LLM 추상화 계층 (`LlmClient`)

서비스 코드는 provider를 모른다. `LlmClient` 인터페이스의
`complete(prompt, schema, maxTokens)`만 호출하고, 구현체와 모델은
`ai.provider` / `ai.model` 설정으로 서버 기동 시점에 `AiConfig`에서 고정된다.

```
LlmClient (interface)
  ├─ AnthropicLlmClient   ── Anthropic Java SDK, MessageCreateParams.outputConfig(schema)
  └─ OpenAiLlmClient      ── OpenAI 호환 provider
```

- `<T> T complete(String prompt, Class<T> schema, long maxTokens)`
  - `schema`: 구조화 출력 스키마 클래스(Jackson 애노테이션으로 필드 설명 부여)
  - 반환: 파싱된 결과. 응답에 구조화 컨텐츠가 없으면 `null`
  - 네트워크·API·파싱 예외는 그대로 전파(호출부에서 폴백/에러 처리)
- `AnthropicLlmClient`는 SDK의 `MessageCreateParams.outputConfig(schema)`로 structured output을
  켜고, 응답의 첫 텍스트 블록을 파싱된 타입으로 돌려준다. 기본 모델은 `claude-opus-4-8`.

## 주요 파일

| 파일 | 역할 |
|---|---|
| `ai/controller/AiController.java` | `/api/ai/analyze`, `/api/ai/match` 엔드포인트 |
| `ai/service/ActivityAnalysisService.java` | A-01 활동 분석 |
| `ai/service/SpaceMatchingService.java` | A-02 하드 필터 + A-03 점수 + A-04 재실행 + 규칙 폴백 |
| `ai/llm/LlmClient.java` | LLM 호출 추상화 인터페이스 |
| `ai/llm/AnthropicLlmClient.java` | Anthropic(Claude) 구현체 |
| `ai/llm/OpenAiLlmClient.java` | OpenAI 호환 구현체 |
| `ai/config/AiConfig.java` | provider/model 설정에 따라 `LlmClient` 빈 결정 |
| `ai/dto/AnalyzeRequest.java` | A-01 요청 |
| `ai/dto/AnalyzedRequirement.java` | A-01 structured output 스키마 |
| `ai/dto/RequirementResponse.java` | A-01 응답(= A-02/A-03 입력 조건) |
| `ai/dto/MatchRequest.java` | A-02/A-03 요청(`excludeSpaceIds` 포함) |
| `ai/dto/SpaceScores.java` | A-03 structured output 스키마 |
| `ai/dto/SpaceMatchResponse.java` | A-03 응답(점수·이유·`aiScored` 포함) |
| `ai/repository/SpaceMatchRepository.java` | A-02 후보 조회(읽기 전용) |
| `ai/repository/SpaceSlotMatchRepository.java` | A-02 슬롯 시간 겹침 조회 |

## 실행 환경

AI 기능은 provider에 맞는 API 키 환경변수가 필요하다
(Anthropic이면 `ANTHROPIC_API_KEY`, Anthropic Java SDK가 자동 인식).
