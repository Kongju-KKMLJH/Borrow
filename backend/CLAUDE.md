# Borrow Backend — 팀 공용 에이전트 가이드

유휴공간 대여 서비스의 Spring Boot 백엔드. 해커톤 MVP이므로 **빠르고 단순하게**, 과한 추상화 금지.

관련 문서: 루트 `CLAUDE.md` (모노레포 전체·프론트 현황) / 루트 `DEPLOYMENT.md` (CI/CD·가비아 배포 계획) / `frontend/BUILD.md` (프론트 로컬 환경)

## 서비스 핵심 플로우

취미 모임 개설(일반 회원) → AI 공간 매칭 → 개최 요청 전송 → 공간 제공자 승인 → 활동 자동 공개(S-01) → 다른 회원 참여 신청

## 인증 — ID/PW 로그인 (HTTP Basic)

초기 계획의 게스트 UUID(`X-Guest-Id` 헤더)는 **폐기됐다.** 팀 회의로 로그인 기능을 도입했다 (PR #28·#38, `auth/` 패키지).

- **HTTP Basic + STATELESS.** 세션·토큰 없음. 매 요청에 `Authorization: Basic base64(loginId:password)`.
- **별도 로그인 API가 없다.** `POST /api/auth/signup`으로 가입하고, `GET /api/auth/me`가 200이면 로그인 성공(401이면 실패)으로 판정한다.
- 비밀번호는 **BCrypt 해시로만 저장**하고, 응답 DTO에 절대 담지 않는다 (`AppUser` 엔티티 노출 금지).
- 인가 규칙(경로별 권한)은 `common/config/SecurityConfig` **한 곳에만** 모은다.
- `@GuestId String guestId` 파라미터는 이름과 달리 이제 **SecurityContext의 로그인 아이디**를 돌려준다 (레거시 이름 유지). 비로그인 열람 허용 API만 `@GuestId(required = false)`.

### 회원 유형과 역할

| 명세 용어 | 코드 | 하는 일 |
|---|---|---|
| 일반 회원 | `Role.MEMBER` | 취미 모임(HOBBY) 개설, 공개된 활동에 참여 |
| 공간 제공자 | `Role.HOST` | 유휴공간·유휴 시간대 등록, 개최 요청 승인/거절 |
| 예술가 | `Role.ARTIST` | MEMBER가 하는 일 전부 + 원데이클래스(CLASS) 개설 |

역할은 **회원가입 시 하나만 선택**하고 변경 API는 만들지 않는다. 한 계정이 두 역할을 겸하지 않는다.

**역할이 가르는 것은 "무엇을 개설·관리할 수 있는가"뿐이다.** 활동 목록·상세 열람과 참여·취소, `/api/me/**`는 **역할과 무관하게 로그인한 모두에게** 허용한다. 그러지 않으면 HOST 계정이 자기 서비스의 활동에 참여조차 못 하고, 시연 중 계정을 갈아끼워야 한다.

### `SecurityConfig` 작성 규칙 (버그 다발 지점)

- 규칙은 **위에서부터 먼저 매칭되는 것이 이긴다.** 비로그인 `GET` 줄이 `/api/spaces/**` HOST 줄보다 위에 있어야 목록 조회가 막히지 않는다. `/api/spaces/mine`처럼 `{id}` 패턴에 삼켜지는 경로는 공개 GET 줄보다 먼저 둔다.
- **HTTP 메서드를 반드시 명시한다.** 경로만 쓰면 같은 URL의 비로그인 `GET`(활동 상세 등)까지 함께 잡힌다.
- 활동 관련 규칙은 `hasRole("MEMBER")`가 아니라 전부 `hasAnyRole("MEMBER", "ARTIST")` — 아니면 예술가가 자기 활동을 못 고친다.
- ARTIST 전용 엔드포인트는 없다. 같은 API를 쓰고 **서버가 역할을 보고 CLASS로 분기**한다.

## 용어

| 용어 | 코드 |
|---|---|
| 회원 계정 | `AppUser` — Spring Security의 `User`와 헷갈리므로 **클래스명은 반드시 `AppUser`** |
| 활동 유형: 취미 모임 / 전문 클래스 | `ActivityType.HOBBY` / `ActivityType.CLASS` |
| 분야: 그림 / 촬영 | `ActivityField.ART` / `ActivityField.PHOTO` |
| 인증 예술가 여부 (F-01) | `Activity.hostCertified` — **개설자 역할이 ARTIST일 때 true** |

MEMBER가 개설하면 `type=HOBBY`, `hostCertified=false`. ARTIST가 개설하면 `type=CLASS`, `hostCertified=true`.
이 분기는 **서버가 로그인 역할을 보고 정한다.** 요청 DTO에 `type`·`hostCertified` 필드를 추가하지 마라 (클라이언트가 배지를 위조하게 된다).

**닉네임도 같은 원칙으로 서버가 채운다.** `Activity.hostNickname`과 `Participation.nickname`은 로그인한 `AppUser.nickname` 값으로 서버가 채운다. 요청 DTO에 닉네임 필드를 두면 남의 이름을 사칭할 수 있다.

## API 표면 (구현 완료 기준)

- 인증: `POST /api/auth/signup`(비로그인 가능), `GET /api/auth/me`
- 활동: 개설 `POST /api/activities` / 목록·상세 `GET`(비로그인 허용) / **수정 `PUT`·삭제 `DELETE /api/activities/{activityId}`** (PR #51) / 요구조건 `PATCH .../requirement`(U-08) / 참여 `.../participations` / 개최요청 `.../hosting-request`
- 내 활동: `/api/me/**` — 로그인 전체
- 공간: `/api/spaces/**`, `/api/host/**` — HOST (목록·상세·슬롯 `GET`은 비로그인 허용)
- AI: `/api/ai/**` — 로그인 전체
- 업로드: `POST /api/uploads`(로그인), 서빙 `/files/**`(공개)

활동 **수정·삭제 규칙** (설계 확정, 코드에 반영됨):

- `DRAFT`/`REJECTED` 상태에서만, **개설자 본인만** 가능. `PENDING`(심사 중)·`PUBLISHED`(참여자 존재)는 범위 밖 — 취소·환불 정책 확정 후 별도 이슈.
- 수정은 `PUT`(전체 교체). **요구조건(`SpaceRequirement`)은 `PUT` 대상이 아니다** — U-08 `PATCH`가 담당하며 진입점을 둘로 만들지 않는다.
- `type`·`hostCertified`·`hostNickname`·`guestId`·`status`는 수정 불가 — 요청 DTO(`ActivityUpdateRequest`)에 되살리지 마라.
- 소유권 검사는 역할 검사와 별개로 서비스에서 `activity.getGuestId().equals(guestId)`로 확인하고 아니면 `FORBIDDEN`.

> ⚠️ **기능코드(U-xx)를 임의로 붙이지 마라.** U-09·U-10이 비어 있으나 무엇인지 확인되지 않았다. 확인 전에는 `@Operation(summary = "활동 수정")`처럼 코드 없이 둔다.

## 🔐 보안 규칙 (필수)

**민감 정보는 절대 소스에 커밋하지 않는다.**

- `application.yml`을 포함한 `*.yml` / `*.yaml`은 `.gitignore`로 커밋 금지 (`docker-compose.yml`만 예외로 추적).
- 실제 값은 `.env`로 관리하고, `application.yml`에는 `${DB_URL}`, `${DB_PASSWORD}` 형태의 **플레이스홀더만** 둔다.
- **`${VAR:실제값}` 기본값 패턴 금지 (팀 규칙).** 이 형태가 키 유출을 만든다 — 실키가 기본값으로 박혀 있으면 로컬 Docker 이미지 빌드 시 이미지에 구워진다. 이미 노출된 자격 증명은 **로테이션**한다.
- 이미 커밋된 파일은 `.gitignore` 추가만으로 빠지지 않는다. `git rm --cached <파일>`로 추적을 끊는다.
- 데모 계정 아이디/비번을 소스에 하드코딩하지 않는다. 로그에 `Authorization` 헤더나 비밀번호를 찍지 마라.
- LAN 평문 HTTP 전제이므로 **실제 개인정보를 넣고 시연하지 않는다.**

## 컨벤션

- **응답**: 모든 컨트롤러는 `ApiResponse.ok(data)`로 감싼다. 에러는 던지기만 하면 `GlobalExceptionHandler`가 처리. 401/403은 필터에서 나므로 `SecurityConfig`가 같은 `{success, data, error}` 포맷으로 직접 써 준다.
- **예외**: `throw new BusinessException(ErrorCode.XXX)`. 새 에러코드는 `ErrorCode` enum의 해당 도메인 섹션에 추가. **같은 상태 조건에는 기존 코드를 재사용**한다 — 상태별로 코드를 새로 파면 프론트가 분기를 두 벌 짜야 한다.
- **DTO**: Java record. 요청은 `XxxRequest`, 응답은 `XxxResponse`. 컨트롤러 밖으로 엔티티 노출 금지.
- **패키지 구조**: `{도메인}/controller`, `{도메인}/service`, `{도메인}/repository`, `{도메인}/dto` — 현재 도메인: `auth`, `activity`, `space`, `ai`, `common`, `domain`(엔티티).
- **엔티티**: setter 금지, 의미 있는 도메인 메서드로 상태 변경 (예: `request.approve()`). 검증 로직은 복사하지 말고 한 곳으로 뽑아 공유한다 (예: 시간 순서 검증은 개설·수정이 함께 사용).
- **Swagger**: `@SecurityScheme(type = HTTP, scheme = "basic")` Authorize 버튼 사용.
- 기술 스택 주의: Spring Boot 4.1 / Spring Security 7(람다 DSL만, `.and()` 불가) / Jackson 3(`tools.jackson` 패키지).

## 테스트 (필수 — 예외 없음)

로그인·인가 도입을 계기로 '선택'에서 **'필수'로 전환**됐고, 전 레이어(`domain`/`common`/`auth`/`activity`/`space`/`ai`)에 테스트가 이미 깔려 있다. 새 기능은 테스트와 함께 온다. Swagger 수동 확인은 보조 수단일 뿐이다.

테스트는 **DB·도커 없이** 돌아간다. 컨텍스트가 필요한 테스트는 인메모리 H2를 애노테이션 안에서 직접 지정한다 — `application.yml`은 커밋되지 않으므로 여기에 의존하면 안 된다.

| 층위 | 방식 | 예 |
|---|---|---|
| 엔티티·DTO | 순수 JUnit + AssertJ | `ActivityTest`, `SpaceDtoTest` |
| 서비스 | Mockito (`@ExtendWith(MockitoExtension.class)`) | `ActivityServiceTest` |
| 컨트롤러 | `@WebMvcTest` + `@Import({SecurityConfig, TestUsers})` | `SpaceControllerTest` |
| 리포지토리 쿼리 | `@RepositoryTest`(= `@DataJpaTest` + H2) | `ActivityRepositoryTest` |
| 인가 매트릭스 | `@IntegrationTest`(= `@SpringBootTest` + H2 + MockMvc) | `SecurityConfigTest` |

> ⚠️ 컨트롤러 테스트에서 `@WithMockUser`는 동작하지 않는다. `SecurityConfig`가 세션을 만들지 않아(STATELESS) 주입한 SecurityContext가 필터체인까지 전달되지 않는다. `support/TestUsers`의 `.with(TestUsers.member()/host()/artist())`로 **실제 Basic 인증 헤더**를 실어 보낸다.
>
> 인가 규칙을 추가하면 `SecurityConfigTest` 매트릭스에 반영하고, **기존 공개 `GET`이 여전히 비로그인 200인지 회귀 검증**한다.

## AI 매칭 (`ai/`)

**Anthropic 전용이 아니다.** PR #19로 LLM provider 선택이 도입됐다 (`AiConfig`).

- `ai.provider` = `anthropic`(기본) | `openai` | `gemini`. 서버 기동 시 고정.
- `ai.model` 비우면 provider별 기본값: anthropic=`claude-opus-4-8`, openai=`gpt-4o-mini`, gemini=`gemini-flash-latest`. **provider를 바꾸면 모델도 함께 맞춰야 한다** — `AI_PROVIDER=gemini`에 OpenAI 모델명을 보내면 실패한다.
- `gemini`는 Google의 OpenAI 호환 엔드포인트로 `OpenAiLlmClient`를 재사용한다 (별도 SDK 불필요, 무료 티어 가능). **운영 배포는 gemini 사용 예정** (`DEPLOYMENT.md`).
- **API 키가 없어도 서버는 뜬다.** A-03 매칭은 규칙 기반 폴백, A-01 분석만 `AI_ANALYSIS_FAILED`로 실패한다. 키 문제가 다른 도메인 개발·배포를 막지 않는다.
- A-02는 LLM이 아니라 JPA 쿼리 하드 필터(지역, 수용인원, 허용분야, 슬롯 시간 겹침)다.

## 명령어

```bash
docker compose up -d          # MySQL 기동 (최초 1회)
./gradlew bootRun             # 서버 실행 → http://localhost:8080/swagger-ui.html
./gradlew compileJava         # 커밋 전 필수 빌드 확인
./gradlew test                # PR 전 필수 (compileJava와 함께 품질 게이트, DB 불필요)

curl -u myid:mypw http://localhost:8080/api/auth/me   # 인증 확인
```

## CI (GitHub Actions)

`.github/workflows/ci.yml` (PR #37) — `main`/`dev`/`backend`/`yonggyu/backend` 대상 PR과 push마다 `compileJava` + `test`를 자동 실행하고 테스트 리포트를 아티팩트로 올린다. **CI가 빨간 PR은 머지하지 않는다.** 프론트엔드 job과 배포(CD) 워크플로는 `DEPLOYMENT.md` Part 1에 계획만 있고 아직 없다.

## Git 규칙 (이슈 기반 워크플로우)

모든 작업은 **이슈 발급 → 브랜치 분기 → 작은 단위 커밋/푸시 → PR** 순서.

브랜치는 **개인 작업 공간을 낀 다단 구조**다. 각 단계의 대상을 섞지 마라.

```
<이름>/feat/*  →  <이름>/backend  →  backend  →  dev  →  main
   (작업)         (개인 작업 공간)   (팀 공용 통합)  (통합 검증)  (릴리스)
```

- `<이름>/backend`(예: `yonggyu/backend`, `kang/backend`)는 **개인 작업 공간**이다. 기능 브랜치의 분기 기준이자 기능 PR의 base다.
- `backend`는 팀 공용 통합 브랜치다. **직접 머지·푸시 금지.** `<이름>/backend → backend` 통합 PR은 팀 합의 시점에만 연다 (예: PR #38, #46).
- `backend → dev`, `dev → main` 승격도 팀 합의 시점에만 (예: PR #39, #25).
- 프론트엔드는 `front` 브랜치에서 작업하고 `dev`를 base로 PR을 연다 (예: PR #43).

1. **이슈 먼저 발급 (필수).** `.github/ISSUE_TEMPLATE/` 템플릿 사용. 기능 → `Feature`, 버그 → `Bug`. 발급된 이슈 번호를 브랜치·커밋·PR에 사용.
2. **브랜치.** `<이름>/backend`에서 `<이름>/feat/<기능>`(버그는 `<이름>/fix/<버그>`) 분기.
   ⚠️ `<이름>/backend/<...>` 형태는 Git이 거부한다 — `refs/heads/<이름>/backend`가 이미 존재하면 하위 ref를 만들 수 없다.
3. **커밋.** `[#이슈번호] 메시지` 형태로 작은 단위마다 커밋·푸시. 한 커밋 = 한 논리 단위.
4. **기능 PR.** `.github/PullRequestTemplate.md` 템플릿, 제목 `[#이슈번호] 작업내용`, base **`<이름>/backend`**, 본문에 `Closes #<이슈번호>`.
5. **품질 게이트.** PR 전 `./gradlew compileJava && ./gradlew test` 통과 필수. CI가 같은 것을 다시 검증한다.

## 진행 중인 작업 (2026-08-18 기준)

- **프론트 기준 코드베이스는 PR #43 (`front → dev`)이다.** 구 `X-Guest-Id` API 레이어를 삭제하고 HTTP Basic으로 전면 교체, Figma 16화면 구현. 결제(`POST /api/activities/{id}/payment`, `GET .../settlement`)·구독(`/api/subscriptions`) 화면은 **UI만** — 해당 백엔드 API는 미구현이며, 만들 때는 루트 `CLAUDE.md`의 API 갭 표와 PR #43의 `frontend/docs/SCREEN_API_MAPPING.md`를 계약 기준으로 삼는다.
- **PR #46** (`kang/backend → backend`, 열림): 가격 항목·예상 운영 수익 표시(#44) + 공간 유휴시간 수정 API·개최요청 일정 불일치 식별(#47). `HostingRequestResponse.PriceBreakdown`, `PlatformFeeProperties`(`platform.fee.*`) 추가. **팀 합의 전 머지 금지** — 추가 커밋 예정.
- **배포 준비**: `DEPLOYMENT.md` Part 1 — Dockerfile·`application-prod.yml`·`deploy/`·CI 프론트 job·CD 워크플로는 계획 단계. 프론트·API를 **단일 오리진**(Caddy 리버스 프록시)으로 배포 예정.

## 알려진 제약

- **`AppUser` 등 테이블 생성은 로컬 `application.yml`의 `ddl-auto`에 달렸다.** 이 파일은 저장소에 없으므로(gitignore) 직접 확인해야 한다. prod에는 `create` 절대 금지 (`DEPLOYMENT.md`).
- actuator 의존성이 없어 `/actuator/health`가 없다 — 헬스체크는 공개 `GET /api/activities`로 대체 중.
- 공개(`PUBLISHED`)된 활동의 수정·취소는 취소·환불 정책이 정해진 뒤 별도 이슈로 다룬다 `(미확정 — 결정 필요)`.
