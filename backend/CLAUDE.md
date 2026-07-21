# Borrow Backend — 팀 공용 에이전트 가이드

천안 유휴공간 대여 서비스의 Spring Boot 백엔드. 해커톤 MVP이므로 **빠르고 단순하게**, 과한 추상화 금지.

## 서비스 핵심 플로우
활동 개설(게스트) → AI 공간 매칭 → 개최 요청 전송 → 공간 제공자 승인 → 활동 공개 → 게스트 참여 신청

로그인 없음. 사용자 식별은 프론트가 생성한 UUID를 `X-Guest-Id` 헤더로 전달받는다.

## ⚠️ 소유권 경계 (가장 중요한 규칙)

세 명이 각자 브랜치에서 병렬 작업 중이다. **담당 패키지 밖의 파일은 읽기만 하고 절대 수정하지 마라.**
수정이 꼭 필요하면 코드를 고치지 말고 사용자에게 알리고 멈춰라.

| 패키지 | 담당 | 내용 |
|---|---|---|
| `common/` | A | 응답 포맷, 예외, 게스트 인터셉터, 설정 — **동결됨, A만 수정** |
| `domain/` | 전원 합의 | 엔티티 전체 — **동결됨, 변경은 구두 합의 후 반영** |
| `ai/` | A | A-01 활동 분석, A-02/A-03 공간 매칭·적합도, A-04 대체 추천 |
| `space/` | B | 공간 CRUD, 유휴시간, 개최요청 승인/거절, 사업자 홈 (B-01~B-11) |
| `activity/` | C | 활동 개설/목록/검색/상세, 개최요청 전송/상태, 참여/취소, 내 활동 (U-01~U-14) |

- `build.gradle`, `application.yml` 변경도 A 담당. 필요하면 요청할 것.
- `HostingRequest` 엔티티는 B 소유. C는 생성(U-11)/조회(U-12) API만 얹는다.

## 컨벤션

- **응답**: 모든 컨트롤러는 `ApiResponse.ok(data)` 로 감싼다. 에러는 던지기만 하면 `GlobalExceptionHandler`가 처리.
- **예외**: `throw new BusinessException(ErrorCode.XXX)`. 새 에러코드는 `ErrorCode` enum에 추가(자기 도메인 섹션에만).
- **게스트 식별**: 컨트롤러 파라미터에 `@GuestId String guestId`. 헤더 없으면 자동으로 400.
- **DTO**: Java record 사용. 요청 DTO는 `XxxRequest`, 응답은 `XxxResponse`. 컨트롤러 밖으로 엔티티 노출 금지.
- **패키지 내부 구조**: `{도메인}/controller`, `{도메인}/service`, `{도메인}/repository`, `{도메인}/dto`
- **엔티티**: setter 금지, 의미 있는 도메인 메서드로 상태 변경 (예: `request.approve()`).
- 테스트 작성은 선택 (해커톤). 대신 Swagger로 직접 호출해 확인.

## API 경로 규약

- 일반 사용자(C): `/api/activities/...`, `/api/me/...`
- 공간 제공자(B): `/api/spaces/...`, `/api/host/...`
- AI(A): `/api/ai/...`

## 명령어

```bash
docker compose up -d          # MySQL 기동 (최초 1회)
./gradlew bootRun             # 서버 실행 → http://localhost:8080/swagger-ui.html
./gradlew compileJava         # 커밋 전 필수 빌드 확인
```

AI 기능(A 담당)은 `ANTHROPIC_API_KEY` 환경변수가 필요하다 (Anthropic Java SDK가 자동 인식).

## Git 규칙

- 작업 브랜치: `feat/ai`(A), `feat/space`(B), `feat/activity`(C). 분기/머지 대상은 **`backend` 브랜치** (main 아님).
- 작업 시작 전: `git pull --rebase origin backend`
- 기능 하나 완료 시마다(2~3시간 단위) backend에 머지. 오래 묵히지 말 것.
- **머지 전 `./gradlew compileJava` 통과 필수. backend 브랜치는 항상 컴파일되는 상태를 유지한다.**
- `backend → main` 머지는 팀 합의 시점에만.

## AI 매칭 구현 방침 (A 참고)

- A-01: Claude API 1회 호출로 활동 설명 → 구조화 조건(분야/인원/시설). Java SDK의 structured output(`outputConfig(클래스)`) 사용, 모델은 `claude-opus-4-8`.
- A-02: LLM 아님 — JPA 쿼리 하드 필터 (지역, 수용인원, 허용분야, 슬롯 시간 겹침).
- A-03: 필터 통과 후보들을 한 번의 Claude 호출로 점수+추천이유 산출. **API 실패 시 규칙 기반 점수로 폴백 필수** (데모 안정성).
- A-04: 거절된 공간 제외 후 A-02+A-03 재실행.
