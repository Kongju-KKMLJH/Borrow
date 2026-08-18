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

## 활동 수정·삭제 (기능명세 2.1)

기능명세 2.1은 "프로그램 개설과 **수정·삭제**"인데 현재 개설만 있다. 고칠 수 있는 것은 요구조건(U-08)뿐이라 **제목·일정·정원·참가비를 잘못 넣으면 활동을 새로 만드는 수밖에 없다.**

| 대상 | 엔드포인트 | 권한 |
|---|---|---|
| 활동 수정 | `PUT /api/activities/{activityId}` | `MEMBER`, `ARTIST` + **개설자 본인** |
| 활동 삭제 | `DELETE /api/activities/{activityId}` | `MEMBER`, `ARTIST` + **개설자 본인** |

> ⚠️ **기능코드(U-xx)를 임의로 붙이지 마라.** 현재 문서의 U 코드는 U-01~U-08, U-11~U-14뿐이고 **U-09·U-10이 비어 있으나 그것이 무엇인지는 확인되지 않는다.** 명세 담당자에게 확인한 뒤 `@Operation(summary = ...)`에 반영한다. 확인 전에는 `@Operation(summary = "활동 수정")`처럼 코드 없이 둔다.

### 메서드와 경계

- **수정은 `PUT`**(전체 교체)이다. `PUT /api/spaces/{spaceId}`와 같은 형태를 따른다.
- **요구조건(`SpaceRequirement`)은 이 API의 대상이 아니다.** `PATCH /api/activities/{activityId}/requirement`(U-08)가 이미 담당한다. **`PUT` 본문에 `region`·`headcount`·`requiredFacilities`·`noisy`·`messy`를 넣지 마라** — 진입점이 둘이 되면 어느 쪽이 최종값인지 갈린다.

### 수정 가능한 필드

| 필드 | 수정 | 비고 |
|---|---|---|
| `field`, `title`, `description`, `imageUrls` | ⭕ | |
| `date`, `startTime`, `endTime` | ⭕ | 시간 순서 검증 대상 |
| `capacity`, `entryFee` | ⭕ | |
| `type`, `hostCertified`, `hostNickname` | ❌ | **서버가 로그인 역할·계정에서 정한다** |
| `guestId`, `status`, `requirement` | ❌ | 소유자·상태·요구조건은 다른 경로로만 바뀐다 |

> ⚠️ **요청 DTO(`ActivityUpdateRequest`)에 ❌ 필드를 두지 마라.** `ActivityCreateRequest`가 이미 같은 원칙으로 `type`·`hostCertified`·닉네임을 뺐다. 여기서 되살리면 클라이언트가 배지와 표시 이름을 위조한다.

### 상태 조건 — `DRAFT` / `REJECTED`에서만

`PENDING`·`PUBLISHED` 활동의 수정·삭제는 **이번 범위 밖이다.**

- `PENDING`: 공간 제공자가 심사 중인 내용이 바뀌면 승인 근거가 달라진다.
- `PUBLISHED`: 참여자가 이미 신청했다. 취소·환불 정책이 확정되지 않았으므로 손대지 않는다.

> ⚠️ **새 에러코드를 만들지 마라.** U-08(요구조건 수정)이 **똑같은 상태 조건**에서 이미 코드를 던지고 있다. `ActivityService`의 요구조건 수정 메서드를 열어 확인하고 **그 코드를 그대로 재사용**한다. 상태별로 다른 코드를 새로 파면 프론트가 분기를 두 벌 짜야 한다.

### 검증

- **종료 시각 > 시작 시각.** 개설(U-06)과 **같은 규칙**이다. 검증 로직을 복사해 두 벌로 만들지 말고 한 곳으로 뽑아 개설·수정이 함께 쓴다.
- 소유권은 서비스에서 `activity.getGuestId().equals(guestId)` 비교로 확인하고 아니면 `FORBIDDEN`. `SecurityConfig`의 역할 검사와 **별개**다.

> ⚠️ `date`의 `@FutureOrPresent` 누락(이슈 #11)은 **개설 쪽 버그이며 이 브랜치 소관이 아니다.** 다만 **수정 API에서 같은 누락을 반복하지는 마라** — 새로 만드는 DTO에는 처음부터 넣는다.

### 삭제 순서

`DELETE /api/spaces/{spaceId}`가 슬롯을 먼저 지우는 패턴과 **동일하다.**

1. 활동 조회 → 없으면 `ACTIVITY_NOT_FOUND`
2. 개설자 본인 확인 → 아니면 `FORBIDDEN`
3. 상태 확인 → `DRAFT`/`REJECTED`가 아니면 거부
4. **개최요청(`HostingRequest`) 먼저 삭제** — `REJECTED` 활동에는 거절된 요청이 남아 있다. 안 지우면 FK 위반으로 삭제가 실패한다
5. 활동 삭제

> ⚠️ **참여(`Participation`)를 임의로 지우지 마라.** 참여는 `PUBLISHED`에서만 생기므로 대상 상태에는 없어야 한다. **정말 없는지 리포지토리로 확인하고, 남아 있으면 삭제를 거부한다.** 남의 신청 내역을 말없이 지우는 코드를 넣지 마라.

> ⚠️ **업로드된 이미지 파일은 지우지 않는다.** `FileStorageService`에 삭제 기능이 있는지 확인되지 않았다. 고아 파일 정리는 별도 이슈다.

- `imageUrls`(`activity_image`)·`requiredFacilities`(`activity_required_facility`) 컬렉션이 함께 지워지는지는 **엔티티의 cascade·orphanRemoval 설정에 달렸다.** 코드에서 직접 확인하고, 안 되어 있으면 삭제 순서에 넣는다.

### `SecurityConfig` 규칙

```java
.requestMatchers(HttpMethod.PUT,    "/api/activities/{activityId}").hasAnyRole("MEMBER", "ARTIST")
.requestMatchers(HttpMethod.DELETE, "/api/activities/{activityId}").hasAnyRole("MEMBER", "ARTIST")
```

> ⚠️ **HTTP 메서드를 반드시 명시한다.** 경로만 쓰면 같은 URL의 **`GET`(비로그인 활동 상세, U-03)까지 함께 잡혀** 목록에서 상세로 못 들어간다. 이 절에서 제일 나기 쉬운 버그다.

> ⚠️ **`hasRole("MEMBER")`로 쓰지 마라.** 예술가가 자기 활동을 못 고친다. 활동 관련 규칙은 전부 `hasAnyRole("MEMBER", "ARTIST")`다.

> ⚠️ 규칙은 **위에서부터 먼저 매칭되는 것이 이긴다.** 메서드가 달라 비로그인 `GET` 줄과 서로 삼키지는 않지만, 두 줄을 기존 활동 규칙 옆에 붙여 두고 순서를 눈으로 확인한다.

### 테스트 (필수 — 예외 없음)

| 층위 | 무엇을 검증하나 |
|---|---|
| 엔티티 | 수정 도메인 메서드로 대상 필드가 바뀌는가, **`guestId`·`type`·`hostCertified`·`status`는 그대로인가** |
| DTO | `ActivityUpdateRequest`의 검증 애노테이션, 응답 변환(`from`/`of`) |
| 서비스 | 남의 활동 → `FORBIDDEN` / 없는 활동 → `ACTIVITY_NOT_FOUND` / `PENDING`·`PUBLISHED` → 거부 / 종료 ≤ 시작 → 거부 / 삭제 시 **개최요청이 먼저 지워지는가** |
| 컨트롤러 | `@WebMvcTest` + `TestUsers` — `member1` 200, `other-member` 403, 비로그인 401 |
| 인가 매트릭스 | `SecurityConfigTest`에 `PUT`·`DELETE` 추가 + **`GET`이 여전히 비로그인 200인지 회귀 검증** |

> ⚠️ 컨트롤러 테스트에서 `@WithMockUser`는 동작하지 않는다. `support/TestUsers`의 `.with(TestUsers.member()/artist())`로 **실제 Basic 헤더**를 실어 보낸다.

### 함께 갱신할 문서

- `PROJECT_CONTEXT.md` 5.1 — 활동 기능 목록에 수정·삭제 추가
- `PROJECT_CONTEXT.md` 11절 — **"활동 수정·삭제 API 없음" 항목 제거**

> ⚠️ **팀원 작업과 충돌 가능 (2026-08-18 확인).** 이슈 #44(가격 항목·예상 운영 수익 표시)와 #47(슬롯 수정 API·일정 불일치 표시)은 `kang/backend`에만 있고 **`backend`에는 아직 머지되지 않았다** — `origin/backend`는 `yonggyu/backend`와 같은 커밋이다. 통합 PR #46(base `backend` ← head `kang/backend`)은 Draft로 열려 있다.
>
> - **소스 충돌 없음.** #44가 건드린 활동 쪽 파일은 `activity/dto/HostingRequestResponse.java`(가격 `PriceBreakdown` 추가, `from(r)` → `from(r, matchingFee)`)와 `ActivityHostingRequestService` 뿐이다. `ActivityResponse`라는 클래스는 존재하지 않으며, `ActivityDetailResponse`·`ActivitySummaryResponse`·`Activity`·`ActivityService`·`ActivityController`·`SecurityConfig`는 kang 쪽에서 건드리지 않았다.
> - **테스트 한 곳만 주의.** `ActivityDtoTest.java`는 kang이 **파일 끝** `ParticipationAndRequest` 중첩 클래스에 가격 검증을 붙였다. 수정 DTO 테스트를 파일 끝에 추가하면 같은 hunk에서 충돌하므로, `ActivityResponses` 뒤에 끼워 넣거나 별도 파일로 분리한다.

---

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
- **`AppUser` 테이블 생성 방식은 로컬 `application.yml`의 `ddl-auto`에 달렸다.** 이 파일은 저장소에 없으므로(gitignore) 자동 생성 여부는 직접 확인해야 한다.
