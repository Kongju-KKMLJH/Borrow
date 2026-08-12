# Borrow Backend — 에이전트 가이드

유휴공간 대여 및 서비스의 Spring Boot 백엔드. 해커톤 MVP이므로 **빠르고 단순하게**, 과한 추상화 금지.

## 서비스 핵심 플로우

취미 모임 개설(일반 회원) → AI 공간 매칭 → 개최 요청 전송 → 공간 제공자 승인 → 활동 자동 공개(S-01) → 다른 회원 참여 신청

**로그인은 ID/PW 방식이다.** Spring Security + HTTP Basic으로 인증하고, 회원 유형을 역할(Role)로 구분해 권한을 나눈다. 기존 `X-Guest-Id` 헤더 방식은 **폐기**한다.

## 회원 유형과 역할

| 명세 용어 | 코드 | 하는 일 |
|---|---|---|
| 일반 회원 | `Role.MEMBER` | 취미 모임(HOBBY) 개설, 공개된 활동에 참여 |
| 공간 제공자 | `Role.HOST` | 유휴공간·유휴 시간대 등록, 개최 요청 승인/거절 |
| 예술가 | `Role.ARTIST` | MEMBER가 하는 일 전부 + 원데이클래스(CLASS) 개설 |

역할은 **회원가입 시 하나만 선택**하고 변경 API는 만들지 않는다. 한 계정이 두 역할을 겸하지 않는다.

**역할이 가르는 것은 "무엇을 개설·관리할 수 있는가"뿐이다.** 활동 목록·상세 열람과 참여·취소, `/api/me/**`는 **역할과 무관하게 로그인한 모두에게** 허용한다. 그러지 않으면 HOST 계정이 자기 서비스의 활동에 참여조차 못 하고, 시연 중 계정을 갈아끼워야 한다.

## 🔑 인증 / 인가

### 방식: HTTP Basic

매 요청에 `Authorization: Basic base64(로그인아이디:비밀번호)` 헤더를 보낸다. Spring Security가 검증하므로 **커스텀 필터를 짜지 마라.** JWT·세션 쿠키는 채택하지 않는다. 프론트는 기존 `X-Guest-Id` 자리에 `Authorization` 헤더를 넣으면 된다.

> ⚠️ Spring Boot 4.1은 **Spring Security 7.x**를 쓴다. Security 7부터 **람다 DSL이 필수**라 `.and()` 체이닝을 쓰는 옛 예제는 그대로 두면 컴파일이 안 된다.

### 핵심 전략: `@GuestId`를 그대로 둔다

`GuestIdArgumentResolver`가 값을 가져오는 **출처만 헤더 → SecurityContext로** 바꾼다. 그러면 서비스의 소유권 비교 로직과 컨트롤러 시그니처를 **한 줄도 고치지 않는다.**

```java
// GuestIdArgumentResolver.resolveArgument 내부만 교체
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
    if (required) throw new BusinessException(ErrorCode.UNAUTHORIZED);   // 400 GUEST_ID_REQUIRED 아님
    return null;                 // @GuestId(required = false) 는 기존대로 null
}
return auth.getName();           // 로그인 아이디가 guestId 자리에 들어간다
```

- 기존 `GUEST_ID_REQUIRED`(400)를 그대로 쓰지 마라. 같은 "로그인 안 됨" 상황인데 필터의 entryPoint는 401을, 리졸버는 400을 내보내 **응답이 갈린다.** 둘 다 `UNAUTHORIZED`(401)로 통일한다.

- `Activity.guestId` 등 **컬럼·필드 이름은 바꾸지 않는다.** 들어가는 값만 UUID → 로그인 아이디로 바뀐다.
- 컨트롤러는 계속 `@GuestId String guestId`를 쓴다. `@AuthenticationPrincipal`을 새로 도입하지 마라 — 두 방식이 섞이면 소유권 판정이 갈린다.
- **역할·닉네임이 필요하면 `AppUserRepository.findByLoginId(guestId)`로 조회한다.** 없으면 `USER_NOT_FOUND`. `getAuthorities()` 파싱이나 서비스에서의 `SecurityContextHolder` 직접 접근은 금지 — **인가(URL 접근 가능 여부)는 `SecurityConfig`가, 비즈니스 분기(HOBBY냐 CLASS냐)는 `AppUser` 조회가** 담당한다.
  - 조회가 필요한 곳은 활동 개설(role→`type`·`hostCertified`, nickname→`hostNickname`), 참여 신청(nickname), `GET /api/auth/me` 뿐이다. 공간 등록의 `ownerId`는 `guestId` 값을 그대로 쓴다.
  - 여러 패키지에서 `AppUser`를 조회할 때는 **리포지토리 인터페이스 이름을 패키지마다 다르게** 둔다(예: `ActivityUserRepository`) — 빈 이름 충돌 회피, `SpaceMatchRepository`가 쓰는 패턴과 동일.

### 권한 매핑 (SecurityConfig 기준)

| 대상 | 접근 범위 |
|---|---|
| 누구나(비로그인) | `POST /api/auth/signup`, `GET /api/activities`, `GET /api/activities/{id}`, `GET /api/spaces`, `GET /api/spaces/{id}`, `GET /api/spaces/{id}/slots`, `/files/**`, Swagger |
| 로그인 전체(역할 무관) | 활동 참여·취소, `/api/me/**`, `/api/ai/**`, `POST /api/uploads`, `GET /api/auth/me` |
| `MEMBER`, `ARTIST` | 활동 개설, 요구조건 수정, 개최 요청 전송·상태 조회 |
| `HOST` | `/api/spaces/**` 쓰기(등록·수정·삭제·슬롯), `/api/host/**`(요청 목록·승인·거절·홈·일정) |

> ⚠️ **`GET`을 `/**`로 열지 마라.** `GET /api/activities/**`를 통째로 열면 개설자 전용인 `GET /api/activities/{id}/hosting-request`(U-12)까지 비로그인에 노출된다. 위 표처럼 **경로를 하나씩 명시**한다.

> ⚠️ **규칙은 위에서부터 먼저 매칭되는 것이 이긴다.** 비로그인 `GET` 줄을 `/api/spaces/** hasRole("HOST")` 줄보다 **위에** 둬야 목록 조회가 안 막힌다. 이 설정에서 제일 흔한 버그가 순서 실수다.

> ⚠️ **ARTIST는 별도 엔드포인트가 없다.** 활동 개설은 MEMBER와 같은 API를 쓰고 서버가 역할을 보고 CLASS로 분기할 뿐이다. 따라서 활동 관련 규칙은 반드시 **`hasAnyRole("MEMBER", "ARTIST")`** 로 쓴다 — `hasRole("MEMBER")`로 쓰면 예술가가 활동을 못 만든다.

권한 규칙은 `SecurityConfig` **한 곳에** 모은다. `@PreAuthorize`를 서비스마다 흩뿌리지 마라.

**CORS는 `CorsConfigurationSource` 빈으로 옮긴다.** 기존 `WebConfig.addCorsMappings` 설정은 Security 필터체인에 적용되지 않아, 필터체인에 `.cors(Customizer.withDefaults())`만 켜두면 프리플라이트(`OPTIONS`)가 401로 막힌다. 설정을 빈으로 등록해야 필터가 그 값을 읽는다.

### 회원가입 / 로그인

- `POST /api/auth/signup` — 로그인아이디·비밀번호·닉네임·역할. 아이디 중복이면 `DUPLICATE_LOGIN_ID`.
- **로그인 API는 만들지 않는다.** Basic 인증은 매 요청에 자격증명을 보내므로, 프론트의 "로그인 버튼"은 `GET /api/auth/me`를 호출해 200이면 성공 처리한다.
- `GET /api/auth/me` — 인증 확인 겸 내 정보 조회. 공통 응답 래퍼를 그대로 따른다.

  ```json
  { "success": true,
    "data": { "loginId": "hong", "nickname": "홍길동", "role": "MEMBER" },
    "error": null }
  ```

  PK(`id`)와 비밀번호는 절대 담지 않는다. 미인증이면 401 `UNAUTHORIZED`.
- 비밀번호는 **반드시 `PasswordEncoder.encode()`를 거쳐 저장**한다.

### 401 / 403 응답 포맷 (놓치기 쉬움)

401/403은 **컨트롤러에 도달하기 전 필터에서** 발생하므로 `GlobalExceptionHandler`가 못 잡는다. 그대로 두면 프론트가 파싱하는 `{success, data, error}` 형태가 아닌 응답이 나간다. `AuthenticationEntryPoint`(401)와 `AccessDeniedHandler`(403)에서 직접 `ApiResponse.fail(...)` JSON을 써준다. 커스텀 entryPoint를 쓰면 브라우저의 Basic 인증 팝업창도 안 뜬다.

```java
@Bean AuthenticationEntryPoint entryPoint() {
    return (req, res, e) -> writeError(res, ErrorCode.UNAUTHORIZED);
}
@Bean AccessDeniedHandler deniedHandler() {
    return (req, res, e) -> writeError(res, ErrorCode.FORBIDDEN);
}
// writeError(res, code):
//   res.setStatus(code.getStatus().value());
//   res.setContentType("application/json;charset=UTF-8");
//   objectMapper.writeValue(res.getWriter(), ApiResponse.fail(code));
```

코드명·메시지·상태값을 리터럴로 박지 마라. `ErrorCode` enum이 이미 셋을 다 들고 있으므로 그걸 그대로 쓴다 — 문자열을 따로 적으면 enum과 갈라져 프론트가 못 읽는 코드가 나간다.

## 용어

| 용어 | 코드 |
|---|---|
| 회원 계정 | `AppUser` — Spring Security의 `User`와 헷갈리므로 **클래스명은 반드시 `AppUser`** |
| 활동 유형: 취미 모임 / 전문 클래스 | `ActivityType.HOBBY` / `ActivityType.CLASS` |
| 분야: 그림 / 촬영 | `ActivityField.ART` / `ActivityField.PHOTO` |
| 인증 예술가 여부 (F-01) | `Activity.hostCertified` — **개설자 역할이 ARTIST일 때 true** |

MEMBER가 개설하면 `type=HOBBY`, `hostCertified=false`. ARTIST가 개설하면 `type=CLASS`, `hostCertified=true`.
이 분기는 **서버가 로그인 역할을 보고 정한다.** 요청 DTO에 `type`·`hostCertified` 필드를 추가하지 마라 (클라이언트가 배지를 위조하게 된다).

**닉네임도 같은 원칙으로 서버가 채운다.** `Activity.hostNickname`과 `Participation.nickname`은 로그인한 `AppUser.nickname` 값으로 서버가 채우고, 요청 DTO(`ActivityCreateRequest`, 참여 신청 요청)에서 닉네임 필드를 **제거**한다. 클라이언트가 보낸 값을 그대로 쓰면 남의 이름을 사칭할 수 있고, 계정 닉네임과 화면 표시 이름이 갈라진다.

## 🔐 보안 규칙 (필수)

**민감 정보는 절대 소스에 커밋하지 않는다.**

- `application.yml`을 포함한 `*.yml` / `*.yaml`은 `.gitignore`로 커밋 금지 (`docker-compose.yml`만 예외로 추적).
- 실제 값은 `.env`로 관리하고, `application.yml`에는 `${DB_URL}`, `${DB_PASSWORD}` 형태의 **플레이스홀더만** 둔다. 기본값(`${DB_PASSWORD:비밀번호}`)에 실제 값을 넣지 마라.
- 이미 커밋된 파일은 `.gitignore` 추가만으로 빠지지 않는다. `git rm --cached <파일>`로 추적을 끊고, 노출된 자격 증명은 **로테이션**한다.
- **비밀번호는 BCrypt 해시로만 저장**하고, 응답 DTO에 절대 담지 않는다(`AppUser` 엔티티 노출 금지).
- 데모 계정의 아이디/비번을 소스에 하드코딩하지 않는다. 로그에 `Authorization` 헤더나 비밀번호를 찍지 마라.
- LAN 평문 HTTP 전제이므로 **실제 개인정보를 넣고 시연하지 않는다.**

## 컨벤션

- **응답**: 모든 컨트롤러는 `ApiResponse.ok(data)`로 감싼다. 에러는 던지기만 하면 `GlobalExceptionHandler`가 처리.
- **예외**: `throw new BusinessException(ErrorCode.XXX)`. 새 에러코드는 `ErrorCode` enum의 해당 도메인 섹션에 추가. 인증 도입으로 추가되는 코드는 `DUPLICATE_LOGIN_ID`(409), `USER_NOT_FOUND`(404), `UNAUTHORIZED`(401), `FORBIDDEN`(403).
- **인증 주체 식별**: 컨트롤러 파라미터에 `@GuestId String guestId` (값 = 로그인 아이디). 비로그인 열람 허용 API만 `@GuestId(required = false)`.
- **소유권 검증**: 역할 검사와 소유자 검사는 별개다. 역할은 `SecurityConfig`가, "내 것인지"는 서비스에서 `entity.getGuestId().equals(guestId)` 비교로 계속 확인한다.
- **DTO**: Java record. 요청은 `XxxRequest`, 응답은 `XxxResponse`. 컨트롤러 밖으로 엔티티 노출 금지.
- **패키지 구조**: `{도메인}/controller`, `{도메인}/service`, `{도메인}/repository`, `{도메인}/dto`
- **엔티티**: setter 금지, 의미 있는 도메인 메서드로 상태 변경 (예: `request.approve()`).
- **Swagger**: `@SecurityScheme(type = HTTP, scheme = "basic")`으로 Authorize 버튼 사용. 기존 `X-Guest-Id` 헤더 자동 노출 설정은 제거한다.
- **테스트 작성 필수.** 로그인·인증인가 도입을 계기로 '선택'에서 '필수'로 전환. 대상은 인증인가에 한정하지 않고 `domain`/`common`/`auth`/`activity`/`space`/`ai` 전 레이어.
  - **현재 존재하는 모든 엔드포인트**가 대상이다 (새로 추가하는 것만이 아니라 기존 것도 소급 적용). CRUD·조회성 API도 예외 없음.
  - **객체가 생성되는 모든 지점**도 대상이다 — 엔티티 생성(생성자/정적 팩토리), 상태 변경 도메인 메서드, DTO 변환 등.
  - PR 전 `./gradlew compileJava && ./gradlew test` 통과가 품질 게이트. Swagger 수동 확인은 보조 수단일 뿐 테스트를 대체하지 않는다.

## API 경로 규약

- 인증: `/api/auth/...`
- 일반 사용자·예술가: `/api/activities/...`, `/api/me/...`
- 공간 제공자: `/api/spaces/...`, `/api/host/...`
- AI: `/api/ai/...`

## 명령어

```bash
docker compose up -d          # MySQL 기동 (최초 1회)
./gradlew bootRun             # 서버 실행 → http://localhost:8080/swagger-ui.html
./gradlew compileJava         # 커밋 전 필수 빌드 확인
./gradlew test                # PR 전 필수 테스트 (compileJava와 함께 품질 게이트)

curl -u myid:mypw http://localhost:8080/api/auth/me   # 인증 확인
```

테스트는 **DB·도커 없이** 돌아간다. 컨텍스트가 필요한 테스트(`@IntegrationTest`, `@RepositoryTest`, `BorrowApplicationTests`)는
인메모리 H2를 애노테이션 안에서 직접 지정한다 — `application.yml`은 커밋되지 않으므로 여기에 의존하면 안 된다.

테스트 층위별 도구:

| 층위 | 방식 | 예 |
|---|---|---|
| 엔티티·DTO | 순수 JUnit + AssertJ | `ActivityTest`, `SpaceDtoTest` |
| 서비스 | Mockito (`@ExtendWith(MockitoExtension.class)`) | `ActivityServiceTest` |
| 컨트롤러 | `@WebMvcTest` + `@Import({SecurityConfig, TestUsers})` | `SpaceControllerTest` |
| 리포지토리 쿼리 | `@RepositoryTest`(= `@DataJpaTest` + H2) | `ActivityRepositoryTest` |
| 인가 매트릭스 | `@IntegrationTest`(= `@SpringBootTest` + H2 + MockMvc) | `SecurityConfigTest` |

> ⚠️ 컨트롤러 테스트에서 `@WithMockUser`는 동작하지 않는다. `SecurityConfig`가 세션을 만들지 않아(STATELESS)
> 주입한 SecurityContext가 필터체인까지 전달되지 않기 때문이다. 대신 `support/TestUsers`의
> `.with(TestUsers.member()/host()/artist())`로 **실제 Basic 인증 헤더**를 실어 보낸다.

의존성은 `implementation 'org.springframework.boot:spring-boot-starter-security'` 한 줄 추가(버전은 Boot가 관리).
AI 기능은 `ANTHROPIC_API_KEY` 환경변수가 필요하다 (Anthropic Java SDK가 자동 인식).

## Git 규칙 (이슈 기반 워크플로우)

모든 기능 개발·버그 수정은 **이슈 발급 → 브랜치 분기 → 작은 단위 커밋/푸시 → PR** 순서로 진행한다.

브랜치는 **2단 구조**다. 각 단계의 대상을 섞지 마라.

```
yonggyu/feat/*  →  yonggyu/backend  →  backend
   (작업)          (개인 작업 공간)     (팀 공용 통합)
```

- `yonggyu/backend`는 **팀 공용 통합 브랜치가 아니라 개인 작업 공간**이다. 기능 브랜치의 분기 기준이자 기능 PR의 base다.
- `backend`는 팀 공용 통합 브랜치다. **여기에 직접 머지·푸시하지 마라.** `yonggyu/backend → backend` 통합 PR은 팀 합의 시점에 직접 연다.

1. **이슈 먼저 발급 (필수).** 작업 착수 전 `.github/ISSUE_TEMPLATE/`의 템플릿으로 GitHub 이슈를 만든다.
   - 기능 개발 → `Feature`(feature.md) / 외부 요청 작업 → `Feature request`(feature_request.md)
   - 버그 수정 → `Bug`(bug.md) / 질문 → `Question`(question.md)
   - 템플릿의 상세·체크리스트 항목을 채운다. 발급된 **이슈 번호**를 이후 브랜치·커밋·PR에 사용한다.
   - 예: `gh issue create --template feature.md` (gh 인증 필요) 또는 GitHub 웹의 이슈 템플릿.

2. **브랜치.** `yonggyu/backend`에서 이슈 단위로 `yonggyu/feat/<기능이름>` 브랜치를 분기해 작업한다.
   - `git switch yonggyu/backend && git pull --rebase origin yonggyu/backend` 후 `git switch -c yonggyu/feat/<기능이름>`.
   - 버그 수정은 `yonggyu/fix/<버그이름>`.
   - ⚠️ `yonggyu/backend/<...>` 형태는 Git이 거부한다. `refs/heads/yonggyu/backend`가 이미 존재하면 그 아래에 하위 ref를 만들 수 없다.

3. **커밋.** 작은 작업 단위마다 `[#이슈번호] 커밋 메시지` 형태로 커밋하고 바로 push한다.
   - 예: `[#12] A-01 활동 분석 서비스 추가`. 한 커밋 = 한 논리 단위, 큰 덩어리로 몰아 커밋하지 말 것.

4. **기능 PR.** 이슈 단위 작업이 모두 끝나면 `.github/PullRequestTemplate.md` 템플릿으로 PR을 생성한다.
   - 제목 `[#이슈번호] 작업내용`, base 브랜치 **`yonggyu/backend`**.
   - 본문의 `Closes #<이슈번호>`를 채워 머지 시 이슈가 자동으로 닫히게 한다.
   - 예: `gh pr create --base yonggyu/backend --head yonggyu/feat/<기능이름> --title "[#12] AI 공간 매칭" --body-file .github/PullRequestTemplate.md`.

5. **품질 게이트.** PR 올리기 전 `./gradlew compileJava && ./gradlew test` 통과 필수. `yonggyu/backend`와 `backend` 모두 항상 컴파일·테스트가 통과하는 상태를 유지한다.

6. **통합 PR.** `yonggyu/backend → backend` 머지는 **팀 합의 시점에만** 직접 연다. 에이전트가 임의로 `backend`에 머지·푸시하지 않는다.

7. `backend → main` 머지는 팀 합의 시점에만.

## 인증 도입 작업 순서 (이슈 단위로 쪼갤 것)

1. `spring-boot-starter-security` 의존성 추가
2. `domain/AppUser` + `domain/Role`, `Space.ownerId` 추가
3. `AppUserRepository`(`findByLoginId`, `existsByLoginId`) + `AppUserDetailsService`
4. `SecurityConfig` — `PasswordEncoder`, 필터체인, 경로별 권한(순서 주의), Basic, entryPoint/deniedHandler
5. `GuestIdArgumentResolver` 내부를 SecurityContext 기반으로 교체
6. `/api/auth/signup`, `/api/auth/me`
7. `OpenApiConfig` — `X-Guest-Id` 헤더 노출 제거, Basic SecurityScheme 추가
8. 공간·개최요청·사업자 홈에 `ownerId` 소유자 검증 반영, 활동·참여 DTO에서 닉네임 필드 제거
9. 프론트에 헤더 교체(`X-Guest-Id` → `Authorization`) 및 요청 DTO 변경 사항 전달

## 알려진 제약 / 같이 처리할 것

- **`Space.ownerId`를 추가한다 (도입 확정).** 현재 `Space`에는 소유자 필드가 없어, `hasRole("HOST")`만 걸면 로그인한 아무 사업자나 **남의 공간을 수정하고 남의 요청을 승인할 수 있다.** 등록(`POST /api/spaces`) 시 `@GuestId` 값으로 `ownerId`를 채우고, 아래 지점에 소유자 비교를 넣는다 — 활동 쪽이 이미 쓰는 패턴과 동일하다.
  - 공간: `PUT`/`DELETE /api/spaces/{id}`, 슬롯 추가·삭제
  - 개최요청: `/api/host/requests` 목록·상세·승인·거절 — **내 공간에 온 요청만**
  - 사업자 홈: `/api/host/home`, `/api/host/schedules`의 집계 범위를 내 공간으로 한정
  - `GET /api/spaces` 목록·상세는 비로그인 열람이므로 그대로 둔다.
- **ARTIST 역할이 의미를 가지려면 활동 개설 로직 분기가 필요하다.** 현재 `ActivityService`는 `type=HOBBY`, `hostCertified=false` 하드코딩이다. 역할 분기를 넣으면 "CLASS 개설 API 없음"·"hostCertified Mock" 두 TODO가 함께 해소된다.
- **기존 게스트 데이터는 호환되지 않는다.** `Activity.guestId`의 UUID와 새 로그인 아이디는 다른 값이라 소유권 판정이 깨진다. **DB를 비우고 시작하는 게 빠르다.**
- **`AppUser` 테이블 생성 방식은 로컬 `application.yml`의 `ddl-auto`에 달렸다.** 이 파일은 저장소에 없으므로(gitignore) 자동 생성 여부는 직접 확인해야 한다.
