# Borrow Backend — 에이전트 가이드

유휴공간 대여 및 서비스의 Spring Boot 백엔드. 해커톤 MVP이므로 **빠르고 단순하게**, 과한 추상화 금지.

## 서비스 핵심 플로우

취미 모임 개설(일반 회원) → AI 공간 매칭 → 개최 요청 전송 → 공간 제공자 승인 → 활동 자동 공개(S-01) → 다른 회원 참여 신청

## 회원 유형과 역할

| 명세 용어 | 코드 | 하는 일 |
|---|---|---|
| 일반 회원 | `Role.MEMBER` | 취미 모임(HOBBY) 개설, 공개된 활동에 참여 |
| 공간 제공자 | `Role.HOST` | 유휴공간·유휴 시간대 등록, 개최 요청 승인/거절 |
| 예술가 | `Role.ARTIST` | MEMBER가 하는 일 전부 + 원데이클래스(CLASS) 개설 |

역할은 **회원가입 시 하나만 선택**하고 변경 API는 만들지 않는다. 한 계정이 두 역할을 겸하지 않는다.

**역할이 가르는 것은 "무엇을 개설·관리할 수 있는가"뿐이다.** 활동 목록·상세 열람과 참여·취소, `/api/me/**`는 **역할과 무관하게 로그인한 모두에게** 허용한다. 그러지 않으면 HOST 계정이 자기 서비스의 활동에 참여조차 못 하고, 시연 중 계정을 갈아끼워야 한다.

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

> ⚠️ **번호는 유도값이다.** 요구사항 순서와 각 요구사항 아래 기능 배열 순서로 매겼고, **원문에 검증된 것은 `2.1`뿐**이다(이슈 #49 제목 "기능명세 2.1"). 명세에서 항목이 추가·삭제되면 **번호가 밀린다** — 그때는 **이 표를 먼저 고치고** 다른 문서를 맞춘다. 표에 없는 번호를 새로 지어내지 마라.

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

### `SecurityConfig` — **건드리지 마라**

```java
.requestMatchers(HttpMethod.GET, "/api/activities", "/api/activities/{activityId}").permitAll()
.requestMatchers(HttpMethod.GET, "/api/spaces", "/api/spaces/{spaceId}", "/api/spaces/{spaceId}/slots").permitAll()
```

두 줄 모두 **의도대로 열려 있는 것이 맞다**(U-03 비로그인 상세 열람, B-03/B-04 비로그인 공간 열람). 노출은 서비스·DTO 계층에서 막았다. permitAll을 `authenticated()`로 바꾸면 비로그인 탐색이라는 서비스 전제가 깨진다.

### ⚠️ 남은 후속

- **P0-2는 API 계약 변경이다.** 프론트(PR #43)가 공간 목록·상세에서 `address`를 쓰고 있으면 화면이 빈다. 백엔드 소스 충돌은 0건이지만 **계약은 공유하므로 머지 전에 front 담당에게 알린다.**
- 다음 착수는 **P1-1 시민 탐색**(지역·일정 필터 + 확정 공간 + 잔여 인원, 기능명세 4.1)이다. 상세는 `docs/IMPLEMENTATION_PRIORITY.md`.

### ⚠️ 팀원 작업과의 경계 (2026-08-18 확인)

kang의 통합 PR **#46이 Draft OPEN(미머지)**이다. **미머지 브랜치의 클래스를 import 하지 말고, 아래 파일을 건드리지 마라.**

```
activity/dto/HostingRequestResponse.java      activity/service/ActivityHostingRequestService.java
space/dto/HostingRequestResponse.java         space/service/HostingRequestService.java
space/service/HostService.java                space/service/SpaceSlotService.java
space/service/ScheduleMismatchChecker.java    space/controller/SpaceSlotController.java
domain/SpaceSlot.java                         common/config/PlatformFeeProperties.java
+ 그 테스트 9종
```

> ⚠️ **`SpaceDtoTest.java`만 예외다.** kang이 **97~170줄**(`RequestResponse` 중첩 클래스)을 고쳤다. `SpaceResponse` 테스트는 **60~76줄 근처**에 있으니 새 테스트를 그 근처에 넣어라. **파일 끝에 붙이면 같은 hunk에서 충돌한다.**

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

## 알려진 제약 / 같이 처리할 것

- **활동 수정·삭제는 `DRAFT`/`REJECTED`에서만 가능하다 (설계 확정).** `PENDING`은 심사 중, `PUBLISHED`는 참여자가 있어 손대지 않는다. 공개된 활동의 수정·취소는 취소·환불 정책이 정해진 뒤 별도 이슈로 다룬다 `(미확정 — 결정 필요)`.
- **공간 주소를 승인된 개최 요청의 예술가에게 공개할지 미정** `(미확정 — 결정 필요)`. 기능명세 6.1 `rules` 는 "시민과 예술가에게 동 단위까지"라고만 쓴다. **결정 전까지는 소유 HOST 본인에게만** 주소 전문을 준다.
- **`AppUser` 테이블 생성 방식은 로컬 `application.yml`의 `ddl-auto`에 달렸다.** 이 파일은 저장소에 없으므로(gitignore) 자동 생성 여부는 직접 확인해야 한다.
