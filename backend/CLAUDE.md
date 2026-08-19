# Borrow Backend — 팀 공용 에이전트 가이드

유휴공간 대여 서비스의 Spring Boot 백엔드. 해커톤 MVP이므로 **빠르고 단순하게**, 과한 추상화 금지.

관련 문서: 루트 `CLAUDE.md` (모노레포 전체·프론트 현황) / 루트 `DEPLOYMENT.md` (CI/CD·가비아 배포 계획) / `frontend/BUILD.md` (프론트 로컬 환경)

## 서비스 핵심 플로우

취미 모임 개설(일반 회원) → AI 공간 매칭 → 개최 요청 전송 → 공간 제공자 승인 → 활동 자동 공개(S-01) → 다른 회원 참여 신청

## 인증 — ID/PW 로그인 (HTTP Basic)

초기 계획의 게스트 UUID(`X-Guest-Id` 헤더)는 **폐기됐다.** 팀 회의로 로그인 기능을 도입했다 (이슈 #26, PR #28·#38).

- **HTTP Basic + STATELESS.** 세션·토큰 없음. 매 요청에 `Authorization: Basic base64(loginId:password)`.
- **별도 로그인 API가 없다.** `POST /api/auth/signup`으로 가입하고, `GET /api/auth/me`가 200이면 로그인 성공(401이면 실패)으로 판정한다. 프론트도 이 방식으로 자격증명을 검증해 저장한다.
- 비밀번호는 **BCrypt 해시로만 저장**하고, 응답 DTO에 절대 담지 않는다 (`AppUser` 엔티티 노출 금지).
- 인가 규칙(경로별 권한)은 `common/config/SecurityConfig` **한 곳에만** 모은다.
- `@GuestId String guestId` 파라미터는 이름과 달리 이제 **SecurityContext의 로그인 아이디**를 돌려준다 (레거시 이름 유지). 비로그인 열람 허용 API만 `@GuestId(required = false)`.

## 회원 유형과 역할

| 명세 용어 | 코드 | 하는 일 |
|---|---|---|
| 일반 회원 | `Role.MEMBER` | 취미 모임(HOBBY) 개설, 공개된 활동에 참여 |
| 공간 제공자 | `Role.HOST` | 유휴공간·유휴 시간대 등록, 개최 요청 승인/거절 |
| 예술가 | `Role.ARTIST` | MEMBER가 하는 일 전부 + 원데이클래스(CLASS) 개설 |
| 관리자 | `Role.ADMIN` | 관리자 콘솔에서 회원·프로그램·공간 조회와 통제 조치 (기능명세 7) |

역할은 **회원가입 시 하나만 선택**하고 변경 API는 만들지 않는다. 한 계정이 두 역할을 겸하지 않는다.

> ⚠️ **ADMIN은 가입으로 만들 수 없다.** `POST /api/auth/signup`은 비로그인 허용이라 요청의 role을 그대로 믿으면 누구나 관리자가 된다 — `AuthService.signup`이 ADMIN을 400으로 거부하고, 계정은 `AdminAccountInitializer`가 `ADMIN_LOGIN_ID`·`ADMIN_PASSWORD` 환경변수를 읽어 만든다(값이 없으면 아무 것도 하지 않는다). **이 거부를 지우지 마라.**

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

**닉네임도 같은 원칙으로 서버가 채운다.** `Activity.hostNickname`과 `Participation.nickname`은 로그인한 `AppUser.nickname` 값으로 서버가 채우고, 요청 DTO(`ActivityCreateRequest`, 참여 신청 요청)에서 닉네임 필드를 **제거**한다. 클라이언트가 보낸 값을 그대로 쓰면 남의 이름을 사칭할 수 있고, 계정 닉네임과 화면 표시 이름이 갈라진다.

## 기능명세 참조 규칙 (필수)

**아트민 기능명세를 가리킬 때는 목차 번호만 쓴다.** 커밋 메시지·이슈·PR 본문·코드 주석·문서 전부 해당한다.

- ⭕ `기능명세 4.1`, `4.1 프로그램 목록 및 상세 확인`, `6.1 rules`, `3.2 outcome`
- ❌ 명세 도구(Manyfast)의 **내부 식별자** — `F-`·`R-`·`S-` 로 시작하는 6자 코드. 팀원이 읽지 못하고, 도구가 바뀌면 죽는 참조다. **문서에 남기지 마라.**
- 슬롯을 가리킬 때는 **목차 번호 + 슬롯 이름**(`preconditions`·`trigger`·`action`·`outcome`·`exceptions`·`display`·`permissions`·`rules`·`dataSpec`).
- 유저플로우는 섹션 이름으로 가리킨다 (`유저플로우 s6 프로필·계정 설정`).

> ⚠️ **우리 코드의 기능코드는 별개다.** `U-03`·`B-09`·`A-01`·`S-01`·`F-01` 은 Borrow 자체 코드이므로 그대로 쓴다. 헷갈릴 자리에서는 `기능명세 4.1`처럼 접두어를 붙여 구분한다.

### 기능명세 목차 (번호 정의 — 이 표가 기준이다)

| 번호 | 요구사항 / 기능 |
|---|---|
| **1** | 역할 기반 계정과 인증 |
| 1.1 | 역할별 가입과 접근 제어 |
| 1.2 | 예술가 인증 신청과 상태 확인 |
| **2** | 예술가 프로그램 기획과 관리 |
| 2.1 | 프로그램 개설과 수정·삭제 |
| 2.2 | AI 클래스 기획 가이드 |
| 2.2.1 | 운영 조건 보완 질문 |
| **3** | AI 공간 추천과 개최 확정 |
| 3.1 | AI 공간 추천 |
| 3.1.1 | 추천 공간 결과 확인 |
| 3.2 | 추천 공간 선택 및 개최 요청 |
| 3.3 | 매칭 이용료 Mock 결제와 공개 |
| 3.3.1 | 매칭 확정과 프로그램 공개 상태 전환 |
| 3.4 | 프로그램 운영 조건 추출 |
| **4** | 시민 프로그램 탐색과 참여 |
| 4.1 | 프로그램 목록 및 상세 확인 |
| 4.2 | 시민 참여 신청 |
| **5** | 가격 및 수익 구조 |
| 5.1 | 가격 항목과 예상 운영 수익 표시 |
| **6** | 공간 파트너 공간 관리 |
| 6.1 | 공간 등록 및 수정 |
| 6.2 | 이용 가능 시간 관리 |
| **7** | 관리자 콘솔 운영 |
| 7.1 | 회원 관리 |
| 7.1.1 | 전체 회원 목록 조회 |
| 7.1.2 | 임시 회원 데이터 생성·수정·삭제 |
| 7.1.3 | 회원 강제 탈퇴 처리 |
| 7.1.4 | 예술가 인증 승인 처리 |
| 7.2 | 프로그램 관리 |
| 7.2.1 | 전체 프로그램 목록 조회 |
| 7.2.2 | 임시 프로그램 데이터 생성·수정·삭제 |
| 7.2.3 | 프로그램 강제 삭제 처리 |
| 7.3 | 공간 관리 |
| 7.3.1 | 전체 공간 목록 조회 |
| 7.3.2 | 임시 공간 데이터 생성·수정·삭제 |
| 7.3.3 | 공간 강제 삭제 처리 |

> ⚠️ **번호는 유도값이다.** 요구사항 순서와 각 요구사항 아래 기능 배열 순서로 매겼고, **원문에 검증된 것은 `2.1`뿐**이다(이슈 #49 제목 "기능명세 2.1"). 명세에서 항목이 추가·삭제되면 **번호가 밀린다** — 그때는 **이 표를 먼저 고치고** 다른 문서를 맞춘다. 표에 없는 번호를 새로 지어내지 마라.

## API 표면 (구현 완료 기준)

- 인증: `POST /api/auth/signup`(비로그인 가능), `GET /api/auth/me`
- 활동: 개설 `POST /api/activities` / 목록·상세 `GET`(비로그인 허용) / 수정 `PUT`·삭제 `DELETE /api/activities/{activityId}`(기능명세 2.1, 이슈 #49) / 요구조건 `PATCH .../requirement`(U-08) / 참여 `.../participations` / 개최요청 `.../hosting-request`
- 내 활동: `/api/me/**` — 로그인 전체
- 공간: `/api/spaces/**`, `/api/host/**` — HOST (목록·상세·슬롯 `GET`은 비로그인 허용)
- 유휴시간 슬롯: `GET`·`POST /api/spaces/{spaceId}/slots`, 수정 `PUT`·삭제 `DELETE .../slots/{slotId}` (기능명세 6.2, 이슈 #47)
- AI: `/api/ai/**` — 로그인 전체
- 업로드: `POST /api/uploads`(로그인), 서빙 `/files/**`(공개)
- 관리자 콘솔: `/api/admin/**` — ADMIN 전용 (기능명세 7, 이슈 #86)
  - 회원 `GET /api/admin/users`, 강제 탈퇴 `POST .../users/{userId}/withdraw`
  - 인증 심사 `GET /api/admin/artist-verifications`(기본 PENDING), 승인 `POST .../{id}/approve`
  - 프로그램 `GET /api/admin/activities`, 강제 삭제 `POST .../{activityId}/force-delete`
  - 공간 `GET /api/admin/spaces`, 강제 삭제 `POST .../{spaceId}/force-delete`
  - 임시(mock) 데이터 CRUD `POST`·`PUT`·`DELETE /api/admin/{users,activities,spaces}` (기능명세 7.1.2·7.2.2·7.3.2, 이슈 #88)

활동 **수정·삭제 규칙** (설계 확정, 코드에 반영됨):

- `DRAFT`/`REJECTED` 상태에서만, **개설자 본인만** 가능. `PENDING`(심사 중)·`PUBLISHED`(참여자 존재)는 범위 밖.
- 수정은 `PUT`(전체 교체). **요구조건(`SpaceRequirement`)은 `PUT` 대상이 아니다** — U-08 `PATCH`가 담당하며 진입점을 둘로 만들지 않는다.
- `type`·`hostCertified`·`hostNickname`·`guestId`·`status`는 수정 불가 — 요청 DTO(`ActivityUpdateRequest`)에 되살리지 마라.
- 소유권 검사는 역할 검사와 별개로 서비스에서 `activity.getGuestId().equals(guestId)`로 확인하고 아니면 `FORBIDDEN`.

### 가격 표시 (기능명세 5.1, 이슈 #44)

개최 요청 응답(U-11/U-12)에 `PriceBreakdown`으로 참가비 단가·예상 참가비 수익·공간 이용료·매칭 이용료·예상 운영 수익을 함께 내려준다.

> ⚠️ **`platform.fee.matching` 설정이 필요하다.** `PlatformFeeProperties.matching`은 기본값 없는 `int`라 설정을 주지 않으면 **매칭 이용료가 0원으로 표시**된다(부팅은 정상). `application.yml`이 gitignore되므로 **각자 로컬 yml과 배포용 `application-prod.yml`에 `platform.fee.matching: 5000`을 직접 넣어야 한다.** 매칭 이용료를 화면·코드에 하드코딩하지 마라.

## 비공개 정보 노출 차단 (기능명세 4.1 · 6.1) — **구현 완료** (이슈 #53)

명세가 "감추라"고 한 두 가지가 비로그인에게 새고 있던 문제를 막았다. **아래는 되돌리지 말아야 할 규칙이다.**

| 대상 | 규칙 | 어디에 |
|---|---|---|
| 비공개·매칭 미확정 프로그램 상세 | `PUBLISHED`가 아니면 **개설자 본인에게만** 열고, 남에게는 `ACTIVITY_NOT_FOUND`(404) | `ActivityService.detail` |
| 공간 주소 | 공개 응답은 동 단위(`region`)까지, 주소 전문은 **소유 HOST 본인**에게만 | `SpaceResponse.from` / `forOwner` |

- **404이지 403이 아니다.** 403은 "그 id에 뭔가 있다"를 알려준다. 존재 자체를 감추고, 덤으로 새 에러코드도 필요 없다.
- **개설자 본인 예외는 필수다.** 개설 직후 화면·U-13 내 활동 → 상세·수정 화면 진입이 전부 `DRAFT`에서 상세를 읽는다. **HOST에게는 열지 마라** — 개최 요청 심사자는 `GET /api/host/requests/{requestId}`로 이미 받는다.
- 판정 기준은 **"`PUBLISHED`인가"** 다. 결제 대기 상태가 추가돼도(기능명세 3.3) 가드를 다시 손댈 필요가 없다.
- **주소 문자열을 파싱해 동을 잘라내지 마라.** `Space.region`이 이미 동 단위 값이다(예: `"천안시 서북구 불당동"`). 공개 응답에서 `address`를 빼면 명세가 그대로 충족된다.
- `SpaceResponse.forOwner`를 쓰는 곳은 `findMySpaces`·`create`·`update` **뿐이다.** 비로그인 열람인 `findAll`·`findById`에 쓰지 마라.
- 다른 응답 경로는 애초에 주소를 담지 않는다 — `ai/dto/SpaceMatchResponse`(`region`만), `space/dto/HostingRequestResponse.SpaceInfo`(`id`·`name`·`region`), `ScheduleResponse`·`HostHomeResponse`(위치 필드 없음). **주소를 다시 흘리지 않게 이 목록을 유지하라.**
- **예외 하나: `admin/dto/AdminSpaceResponse`는 주소 전문을 담는다** (팀 확정, 2026-08-19). 감추는 대상은 시민·예술가이고 관리자는 통제 대상을 특정해야 하는 주체다. 이 DTO는 `/api/admin/**`(ADMIN 전용)에서만 쓴다 — **다른 경로로 재사용하지 마라.** 규칙 위반으로 보고 필드를 걷어내지도 마라.

### `SecurityConfig` — **건드리지 마라**

```java
.requestMatchers(HttpMethod.GET, "/api/activities", "/api/activities/{activityId}").permitAll()
.requestMatchers(HttpMethod.GET, "/api/spaces", "/api/spaces/{spaceId}", "/api/spaces/{spaceId}/slots").permitAll()
```

두 줄 모두 **의도대로 열려 있는 것이 맞다**(U-03 비로그인 상세 열람, B-03/B-04 비로그인 공간 열람). 노출은 서비스·DTO 계층에서 막았다. permitAll을 `authenticated()`로 바꾸면 비로그인 탐색이라는 서비스 전제가 깨진다.

> 공개 응답에서 `address`가 빠진 것은 **API 계약 변경**이다. 프론트가 공간 목록·상세에서 `address`를 쓰지 않는 것은 확인했다(2026-08-18, PR #55 승격 시점). 앞으로 프론트에 주소 표시가 필요해지면 필드를 되살리지 말고 **소유자 전용 경로를 쓰는지부터** 확인하라.

## 🔐 보안 규칙 (필수)

**민감 정보는 절대 소스에 커밋하지 않는다.**

- `application.yml`을 포함한 `*.yml` / `*.yaml`은 `.gitignore`로 커밋 금지 (`docker-compose.yml`만 예외로 추적).
- 실제 값은 `.env`로 관리하고, `application.yml`에는 `${DB_URL}`, `${DB_PASSWORD}` 형태의 **플레이스홀더만** 둔다.
- **`${VAR:실제값}` 기본값 패턴 금지 (팀 규칙).** 이 형태가 키 유출을 만든다 — 실키가 기본값으로 박혀 있으면 로컬에서 Docker 이미지를 굽는 순간 이미지에 함께 박힌다. 이미 노출된 자격 증명은 **로테이션**한다.
- 이미 커밋된 파일은 `.gitignore` 추가만으로 빠지지 않는다. `git rm --cached <파일>`로 추적을 끊는다.
- **비밀번호는 BCrypt 해시로만 저장**하고, 응답 DTO에 절대 담지 않는다(`AppUser` 엔티티 노출 금지).
- 데모 계정의 아이디/비번을 소스에 하드코딩하지 않는다. 로그에 `Authorization` 헤더나 비밀번호를 찍지 마라.
- **HTTP Basic은 자격증명을 매 요청 평문으로 보낸다**(base64는 암호화가 아니다). 로컬·LAN 평문 HTTP 구간에서는 **실제 개인정보를 넣고 시연하지 않는다.** 외부 배포에서는 HTTPS가 사실상 필수 전제다 (`DEPLOYMENT.md`).

## 컨벤션

- **응답**: 모든 컨트롤러는 `ApiResponse.ok(data)`로 감싼다. 에러는 던지기만 하면 `GlobalExceptionHandler`가 처리. 401/403은 컨트롤러 도달 전 필터에서 나므로 `SecurityConfig`가 같은 `{success, data, error}` 포맷으로 직접 써 준다.
- **예외**: `throw new BusinessException(ErrorCode.XXX)`. 새 에러코드는 `ErrorCode` enum의 해당 도메인 섹션에 추가. 인증 관련 코드는 `DUPLICATE_LOGIN_ID`(409), `USER_NOT_FOUND`(404), `UNAUTHORIZED`(401), `FORBIDDEN`(403). **같은 상태 조건에는 기존 코드를 재사용**한다 — 상태별로 코드를 새로 파면 프론트가 분기를 두 벌 짜야 한다.
- **인증 주체 식별**: 컨트롤러 파라미터에 `@GuestId String guestId` (값 = 로그인 아이디). 비로그인 열람 허용 API만 `@GuestId(required = false)`.
- **소유권 검증**: 역할 검사와 소유자 검사는 별개다. 역할은 `SecurityConfig`가, "내 것인지"는 서비스에서 `entity.getGuestId().equals(guestId)` 비교로 계속 확인한다.
- **DTO**: Java record. 요청은 `XxxRequest`, 응답은 `XxxResponse`. 컨트롤러 밖으로 엔티티 노출 금지.
- **패키지 구조**: `{도메인}/controller`, `{도메인}/service`, `{도메인}/repository`, `{도메인}/dto` — 현재 도메인: `auth`, `activity`, `space`, `ai`, `admin`, `common`, `domain`(엔티티).
- **엔티티**: setter 금지, 의미 있는 도메인 메서드로 상태 변경 (예: `request.approve()`). 검증 로직은 복사하지 말고 한 곳으로 뽑아 공유한다 (예: 시간 순서 검증은 개설·수정이 함께 쓴다).
- **Swagger**: `@SecurityScheme(type = HTTP, scheme = "basic")`으로 Authorize 버튼 사용.
- 기술 스택 주의: Spring Boot 4.1 / Spring Security 7(람다 DSL만, `.and()` 체이닝 불가) / Jackson 3(`tools.jackson` 패키지).
- **테스트 작성 필수.** 로그인·인증인가 도입을 계기로 '선택'에서 '필수'로 전환. 대상은 인증인가에 한정하지 않고 `domain`/`common`/`auth`/`activity`/`space`/`ai` 전 레이어.
  - **현재 존재하는 모든 엔드포인트**가 대상이다 (새로 추가하는 것만이 아니라 기존 것도 소급 적용). CRUD·조회성 API도 예외 없음.
  - **객체가 생성되는 모든 지점**도 대상이다 — 엔티티 생성(생성자/정적 팩토리), 상태 변경 도메인 메서드, DTO 변환 등.
  - PR 전 `./gradlew compileJava && ./gradlew test` 통과가 품질 게이트. Swagger 수동 확인은 보조 수단일 뿐 테스트를 대체하지 않는다.

## API 경로 규약

- 인증: `/api/auth/...`
- 일반 사용자·예술가: `/api/activities/...`, `/api/me/...`
- 공간 제공자: `/api/spaces/...`, `/api/host/...`
- AI: `/api/ai/...`
- 관리자: `/api/admin/...`

## 테스트 도구

테스트는 **DB·도커 없이** 돌아간다. 컨텍스트가 필요한 테스트(`@IntegrationTest`, `@RepositoryTest`, `BorrowApplicationTests`)는
인메모리 H2를 애노테이션 안에서 직접 지정한다 — `application.yml`은 커밋되지 않으므로 여기에 의존하면 안 된다.

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
>
> 인가 규칙을 추가하면 `SecurityConfigTest` 매트릭스에 반영하고, **기존 공개 `GET`이 여전히 비로그인 200인지 회귀 검증**한다.

## AI 매칭 (`ai/`)

**Anthropic 전용이 아니다.** 이슈 #18(PR #19)로 LLM provider 선택이 도입됐다 (`AiConfig`).

- `ai.provider` = `anthropic`(기본) | `openai` | `gemini`. 서버 기동 시 고정.
- `ai.model`을 비우면 provider별 기본값: anthropic=`claude-opus-4-8`, openai=`gpt-4o-mini`, gemini=`gemini-flash-latest`. **provider를 바꾸면 모델도 함께 맞춰야 한다** — `AI_PROVIDER=gemini`에 OpenAI 모델명을 보내면 실패한다.
- `gemini`는 Google의 OpenAI 호환 엔드포인트로 `OpenAiLlmClient`를 재사용한다 (별도 SDK 불필요, 무료 티어 가능). **운영 배포는 gemini 사용 예정** (`DEPLOYMENT.md`).
- **API 키가 없어도 서버는 뜬다.** A-03 매칭은 규칙 기반 폴백, A-01 분석만 `AI_ANALYSIS_FAILED`로 실패한다. 키 문제가 다른 도메인 개발·배포를 막지 않는다.
- A-02는 LLM이 아니라 JPA 쿼리 하드 필터(지역, 수용인원, 허용분야, 슬롯 시간 겹침)다.

## 명령어

```bash
docker compose up -d          # MySQL 기동 (최초 1회)
./gradlew bootRun             # 서버 실행 → http://localhost:8080/swagger-ui.html
./gradlew compileJava         # 커밋 전 필수 빌드 확인
./gradlew test                # PR 전 필수 테스트 (compileJava와 함께 품질 게이트, DB 불필요)

curl -u myid:mypw http://localhost:8080/api/auth/me   # 인증 확인

# 관리자 콘솔(기능명세 7)을 확인하려면 관리자 계정을 환경변수로 만들어 띄운다.
# 두 값이 모두 있고 해당 아이디가 없을 때만 1회 생성된다. 비밀번호를 소스·문서에 적지 마라.
ADMIN_LOGIN_ID=admin ADMIN_PASSWORD='<직접 정한 값>' ./gradlew bootRun
```

> 로컬 `application.yml`에 넣고 싶다면 `admin.login-id` / `admin.password` / `admin.nickname` 키다.
> **`${ADMIN_PASSWORD:실제값}` 형태로 기본값을 박지 마라** (팀 보안 규칙).

## CI (GitHub Actions)

`.github/workflows/ci.yml` (이슈 #36, PR #37) — **job 2개**를 돌린다. **CI가 빨간 PR은 머지하지 않는다.**

- `backend (compile + test)` — `./gradlew compileJava` + `./gradlew test`, 테스트 리포트를 아티팩트로 업로드
- `frontend (typecheck + lint + web build)` — `npx tsc --noEmit` + `npx expo lint` + `npx expo export --platform web`

대상: PR은 `main`/`dev`/`backend`, push는 `main`/`dev`/`backend`/`yonggyu/backend`/`kang/backend`/`front`.
프론트 job은 **웹 번들 빌드까지 검증**하므로 라우트를 추가하면 여기서 함께 걸린다.
배포(CD) 워크플로는 `DEPLOYMENT.md` Part 1에 계획만 있고 아직 없다.

## Git 규칙 (이슈 기반 워크플로우)

모든 기능 개발·버그 수정은 **이슈 발급 → 브랜치 분기 → 작은 단위 커밋/푸시 → PR** 순서로 진행한다.

브랜치는 **개인 작업 공간을 낀 다단 구조**다. 각 단계의 대상을 섞지 마라.

```
<이름>/feat/*  →  <이름>/backend  →  backend  →  dev  →  main
   (작업)         (개인 작업 공간)   (팀 공용 통합)  (통합 검증)  (릴리스)
```

- `<이름>/backend`(예: `yonggyu/backend`, `kang/backend`)는 **팀 공용 통합 브랜치가 아니라 개인 작업 공간**이다. 기능 브랜치의 분기 기준이자 기능 PR의 base다.
- `backend`는 팀 공용 통합 브랜치다. **여기에 직접 머지·푸시하지 마라.** `<이름>/backend → backend` 통합 PR은 팀 합의 시점에 연다 (예: PR #38, #46, #54).
- `backend → dev`, `dev → main` 승격도 팀 합의 시점에만 (예: PR #39, #55).
- 프론트엔드는 `front` 브랜치에서 작업하고 `dev`를 base로 PR을 연다 (예: PR #43).

1. **이슈 먼저 발급 (필수).** 작업 착수 전 `.github/ISSUE_TEMPLATE/`의 템플릿으로 GitHub 이슈를 만든다.
   - 기능 개발 → `Feature`(feature.md) / 외부 요청 작업 → `Feature request`(feature_request.md)
   - 버그 수정 → `Bug`(bug.md) / 질문 → `Question`(question.md)
   - 템플릿의 상세·체크리스트 항목을 채운다. 발급된 **이슈 번호**를 이후 브랜치·커밋·PR에 사용한다.
   - 예: `gh issue create --template feature.md` (gh 인증 필요) 또는 GitHub 웹의 이슈 템플릿.

2. **브랜치.** `<이름>/backend`에서 이슈 단위로 `<이름>/feat/<기능이름>` 브랜치를 분기해 작업한다. 버그 수정은 `<이름>/fix/<버그이름>`.
   - ⚠️ `<이름>/backend/<...>` 형태는 Git이 거부한다. `refs/heads/<이름>/backend`가 이미 존재하면 그 아래에 하위 ref를 만들 수 없다.

3. **커밋.** 작은 작업 단위마다 `[#이슈번호] 커밋 메시지` 형태로 커밋하고 바로 push한다.
   - 예: `[#12] A-01 활동 분석 서비스 추가`. 한 커밋 = 한 논리 단위, 큰 덩어리로 몰아 커밋하지 말 것.

4. **기능 PR.** 이슈 단위 작업이 모두 끝나면 `.github/PullRequestTemplate.md` 템플릿으로 PR을 생성한다.
   - 제목 `[#이슈번호] 작업내용`, base 브랜치 **`<이름>/backend`**.
   - 본문의 `Closes #<이슈번호>`를 채워 머지 시 이슈가 자동으로 닫히게 한다.

5. **품질 게이트.** PR 올리기 전 `./gradlew compileJava && ./gradlew test` 통과 필수. CI가 같은 것을 다시 검증한다.

6. **통합·승격 PR.** 에이전트가 임의로 `backend`·`dev`·`main`에 머지·푸시하지 않는다.

## 현재 상태 (2026-08-18 기준)

- **`dev`가 배포 후보 브랜치다.** PR #55(`backend → dev`)로 백엔드 최신(#44 가격 표시, #47 슬롯 수정·일정 불일치, #49 활동 수정·삭제, #53 정보 노출 차단)이 승격됐고, PR #43으로 프론트 16화면이 들어와 있다. 열린 백엔드 PR은 없다.
- **프론트 기준 코드베이스는 PR #43이다.** 구 `X-Guest-Id` API 레이어를 삭제하고 HTTP Basic으로 전면 교체했다. 결제(`POST /api/activities/{id}/payment`, `GET .../settlement`)·구독(`/api/subscriptions`) 화면은 **UI만** — 해당 백엔드 API는 미구현이며, 만들 때는 루트 `CLAUDE.md`의 API 갭 표와 `frontend/docs/SCREEN_API_MAPPING.md`를 계약 기준으로 삼는다.
- **배포 준비 중.** 서버는 제공받았고, `DEPLOYMENT.md` Part 1 산출물(Dockerfile·`.dockerignore`·`application-prod.yml`·`deploy/`·CD 워크플로)은 아직 만들지 않았다. 프론트·API를 **단일 오리진**(Caddy 리버스 프록시)으로 배포한다.
  - 배포 전 처리 목록: 웹 이미지 업로드 FormData 플랫폼 분기, `app.json` `web.output` → `single`, 웹 같은 오리진 base URL, `platform.fee.matching` 설정, OpenAI 키 로테이션.
- 다음 기능 착수 후보는 **시민 탐색**(지역·일정 필터 + 확정 공간 + 잔여 인원, 기능명세 4.1)이다.

## 관리자 콘솔 (기능명세 7) — 되돌리지 말아야 할 규칙

| 대상 | 규칙 | 어디에 |
|---|---|---|
| ADMIN 계정 생성 | 가입 API로 만들 수 없다. 환경변수 시드(`AdminAccountInitializer`)와 **관리자 콘솔의 임시 회원 생성도 ADMIN을 거부**한다 | `AuthService.signup`, `AdminUserService.create` |
| 강제 탈퇴·강제 삭제 (7.1.3·7.2.3·7.3.3) | **소프트 삭제** — 행을 남기고 `withdrawnAt`/`forceDeletedAt`만 채운다 | `AppUser`·`Activity`·`Space` |
| 임시 데이터 삭제 (7.1.2·7.2.2·7.3.2) | **하드 삭제** — 대신 `mock=true`인 것만, 자식 데이터가 남아 있으면 거절 | `Admin*Service.delete` |
| 인가 | `/api/admin/**` 한 줄만 ADMIN. 기존 MEMBER/HOST/ARTIST 줄에 **ADMIN을 섞지 마라** | `SecurityConfig` |

- **왜 섞으면 안 되나**: 관리자가 개설자 자격으로 남의 활동을 수정할 수 있게 되어, "관리자가 대신 편집하지 않는다"는 기능명세 7.2 범위 제외가 깨진다.
- **임시 프로그램의 상태는 전이 메서드로만 밟는다.** `AdminActivityService.applyStatus`가 `markPending()`→`approve()`→`publish()` 순서를 그대로 탄다. status를 직접 대입하는 setter를 만들면 실제 프로그램의 상태 흐름까지 무너진다. 임시 데이터 전용 진입점은 `resetToDraftByAdmin()`·`forceStatus()`뿐이고 **이름에 `ByAdmin`/`force`를 붙여 실제 경로와 구분**한다.
- **임시 공간에는 이용 가능 시간을 함께 만든다.** A-02가 슬롯 시간 겹침으로 후보를 거르므로, 슬롯 없는 공간은 AI 추천에 걸리지 않는다(기능명세 7.3.2 `outcome` 미충족).

## 알려진 제약 / 같이 처리할 것

- **활동 수정·삭제는 `DRAFT`/`REJECTED`에서만 가능하다 (설계 확정).** `PENDING`은 심사 중, `PUBLISHED`는 참여자가 있어 손대지 않는다. 공개된 활동의 수정·취소는 취소·환불 정책이 정해진 뒤 별도 이슈로 다룬다 `(미확정 — 결정 필요)`.
- **공간 주소를 승인된 개최 요청의 예술가에게 공개할지 미정** `(미확정 — 결정 필요)`. 기능명세 6.1 `rules`는 "시민과 예술가에게 동 단위까지"라고만 쓴다. **결정 전까지는 소유 HOST 본인에게만** 주소 전문을 준다.
- **`AppUser` 등 테이블 생성 방식은 로컬 `application.yml`의 `ddl-auto`에 달렸다.** 이 파일은 저장소에 없으므로(gitignore) 직접 확인해야 한다. **prod에는 `create` 절대 금지** (`DEPLOYMENT.md`).
- actuator 의존성이 없어 `/actuator/health`가 없다 — 배포 헬스체크는 공개 `GET /api/activities`로 대체한다.
- 미해결 버그 이슈: #9(모집 정원이 승인 공간 수용인원과 무관), #8·#30(잘못된 enum·파라미터가 400 대신 500), #11(과거 날짜로 활동 개설 가능 — 개설 DTO에 `@FutureOrPresent` 누락, 수정 DTO에는 있음). 데모를 막지는 않는다.
