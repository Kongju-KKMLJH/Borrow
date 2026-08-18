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

## API 표면 (구현 완료 기준)

- 인증: `POST /api/auth/signup`(비로그인 가능), `GET /api/auth/me`
- 활동: 개설 `POST /api/activities` / 목록·상세 `GET`(비로그인 허용) / 수정 `PUT`·삭제 `DELETE /api/activities/{activityId}`(기능명세 2.1, 이슈 #49) / 요구조건 `PATCH .../requirement`(U-08) / 참여 `.../participations` / 개최요청 `.../hosting-request`
- 내 활동: `/api/me/**` — 로그인 전체
- 공간: `/api/spaces/**`, `/api/host/**` — HOST (목록·상세·슬롯 `GET`은 비로그인 허용)
- 유휴시간 슬롯: `GET`·`POST /api/spaces/{spaceId}/slots`, 수정 `PUT`·삭제 `DELETE .../slots/{slotId}` (기능명세 6.2, 이슈 #47)
- AI: `/api/ai/**` — 로그인 전체
- 업로드: `POST /api/uploads`(로그인), 서빙 `/files/**`(공개)

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

### `SecurityConfig` — **건드리지 마라**

```java
.requestMatchers(HttpMethod.GET, "/api/activities", "/api/activities/{activityId}").permitAll()
.requestMatchers(HttpMethod.GET, "/api/spaces", "/api/spaces/{spaceId}", "/api/spaces/{spaceId}/slots").permitAll()
```

두 줄 모두 **의도대로 열려 있는 것이 맞다**(U-03 비로그인 상세 열람, B-03/B-04 비로그인 공간 열람). 노출은 서비스·DTO 계층에서 막았다. permitAll을 `authenticated()`로 바꾸면 비로그인 탐색이라는 서비스 전제가 깨진다.

> 공개 응답에서 `address`가 빠진 것은 **API 계약 변경**이다. 프론트가 공간 목록·상세에서 `address`를 쓰지 않는 것은 확인했다(2026-08-18, PR #55 승격 시점). 앞으로 프론트에 주소 표시가 필요해지면 필드를 되살리지 말고 **소유자 전용 경로를 쓰는지부터** 확인하라.

## P1 구현 계획 (기능명세 4.1 · 3.1 · 2.2)

우선순위의 **근거**는 `docs/IMPLEMENTATION_PRIORITY.md`에 있다. 여기에는 **구현할 때 지킬 결정**만 적는다.
착수 순서는 **P1-1 → P1-2 → P1-3**이고, P1-2·P1-3은 `ai/**` 안에서 끝나므로 한 브랜치로 묶어도 된다.

> ⚠️ **선행 확인 (2026-08-18, `origin/dev` 병합 시점).** kang의 PR #46과 `backend → dev` 승격(PR #55)이 모두 들어왔다.
> ⑴ "미머지 브랜치의 파일을 건드리지 마라" 제약은 **해제됐다** — `PlatformFeeProperties`·`PriceBreakdown`을 그대로 재사용한다.
> ⑵ `docs/IMPLEMENTATION_PRIORITY.md` 4절이 정한 **3.3 매칭 이용료 결제·공개 + #9 정원 검증의 복귀 조건이 충족**됐다 —
> **P1보다 먼저 할지는 팀 결정 사항이다** `(미확정 — 결정 필요)`.
> ⑶ `ActivityDtoTest`의 줄 위치 회피(213~257줄 hunk)는 **더 이상 필요 없다.**

### P1-1. 시민 탐색 완성 — 지역·일정 필터 · 확정 공간 · 잔여 인원 (기능명세 4.1)

근거는 인수조건 두 줄이다. ① "시민은 **지역·일정·분야**로 공개 프로그램을 탐색할 수 있다" ② "상세에는 인증 예술가, **공간**, 일정, 참가비, **잔여 인원**이 표시된다." 분야·배지·일정·참가비는 이미 있고 **지역·일정 필터, 확정 공간, 잔여 인원 셋이 없다.**

**API 계약 — 추가만 한다.**

| 대상 | 추가 | 비고 |
|---|---|---|
| `GET /api/activities` | 쿼리 `region`, `dateFrom`, `dateTo` | 기존 `type`·`field`·`keyword`와 같은 규약 — **null이면 그 조건 무시** |
| 목록·상세 응답 | `remainingCapacity` | `capacity`·`currentHeadcount`는 **그대로 둔다** (필드 제거는 계약 파괴) |
| 목록·상세 응답 | `space` — 확정 공간 요약 | 확정 전이면 `null` |

**규칙 — 되돌리지 마라.**

1. **지역 기준은 `Activity.requirement.region`(희망 지역)이 아니라 승인된 `Space.region`(실제 개최지)이다.** 시민이 고르는 건 "어디서 열리는가"다. 희망 지역으로 거르면 승인 결과와 다른 동네가 걸린다.
2. **확정 공간 요약은 `id`·`name`·`region`뿐. `address`를 넣지 마라.** 공개 응답의 주소는 동 단위까지라는 기능명세 6.1 `rules` 정책이 여기서 뚫린다. 주소 전문은 소유 HOST 본인에게만 준다(`SpaceResponse.forOwner`).
3. `space`는 **개최 요청이 `APPROVED`일 때만** 채운다. `PENDING`·`REJECTED`는 확정이 아니므로 `null`.
4. `remainingCapacity = max(0, capacity - currentHeadcount)`. 음수를 그대로 내려보내지 않는다.
5. 목록은 지금처럼 **`PUBLISHED`만** 반환한다. 필터를 붙이면서 이 조건을 흔들지 마라.
6. `dateFrom > dateTo`는 `INVALID_REQUEST`. **새 에러코드를 만들지 마라.**
7. `region`은 부분일치(`LIKE '%:region%'`). `Space.region`이 `"천안시 서북구 불당동"` 한 문자열이라 시·구·동 어느 단위로 검색해도 걸린다.

**구현 지점**

| 파일 | 할 일 |
|---|---|
| `activity/repository/ActivityRepository` | `search`에 `region`·`dateFrom`·`dateTo` 추가. 공간 조건은 **`EXISTS` 서브쿼리**로 — join으로 붙이면 요청이 여러 건인 활동이 중복 행으로 나온다 |
| `activity/repository/ActivityHostingRequestRepository` | 확정 공간 단건 조회 + **목록용 배치 조회**(`activityId IN (...)` AND `status = APPROVED`) |
| `activity/service/ActivityService` | 배치 결과를 `Map<activityId, …>`로 만들어 DTO에 넘긴다 |
| `activity/dto/ActivityDetailResponse`·`ActivitySummaryResponse` | `remainingCapacity`, 중첩 record `SpaceInfo(id, name, region)` |
| `activity/controller/ActivityController` | 쿼리 파라미터 3개. 날짜는 `@DateTimeFormat(iso = DATE)`를 **명시**한다 |

**하지 말 것**

- **`Activity`에 `Space` FK를 추가하지 마라.** 관계는 `HostingRequest`가 이미 안다. 엔티티 소유는 공간 도메인이고, 필드를 늘리면 승인·공개(S-01) 시점에 진실이 두 곳으로 갈린다.
- **`space/dto/HostingRequestResponse.SpaceInfo`를 import 하지 마라.** 응답 DTO는 도메인마다 자기 것을 갖는다 — 우리 쪽은 `ActivityDetailResponse` 안의 중첩 record로 만든다.
- **목록에서 활동마다 공간을 개별 조회하지 마라**(N+1). 참여 인원(`currentHeadcount`)의 기존 N+1은 **이번 범위가 아니다** — 같이 고치지 말고 남겨 둔다.

**테스트** (5층 전부, PR 전 `compileJava && test`)

- `@RepositoryTest` — region 부분일치/불일치, 날짜 경계(`dateFrom`·`dateTo` 당일 포함), `APPROVED`가 아닌 요청은 안 걸림, `PUBLISHED`만.
- 서비스 — 미확정이면 `space=null`, `remainingCapacity` 0 하한, 배치 매핑 결과.
- DTO — `SpaceInfo`에 주소 필드가 **없음**(회귀), 잔여 인원 계산.
- 컨트롤러 — 파라미터 바인딩, `dateFrom > dateTo` 400.

### P1-2. 추천 주의사항 + 추천 불가 안내 (기능명세 3.1 `display` · 3.1.1 `exceptions`)

PRD 차별점 문장에 직접 적힌 기능이다 — "AI는 …을 근거로 **추천과 주의사항**을 제공하며, 최종 승인 권한은 공간 파트너에게 남긴다." 지금은 `reason`만 있다.

- `SpaceScores.SpaceScore`와 `SpaceMatchResponse`에 `cautions`(문자열 목록)를 추가한다. 구조화 출력 스키마에는 `@JsonPropertyDescription`으로 "근거 있는 주의사항만"을 못 박는다.
- **규칙 기반 폴백 경로(`ruleReason`)도 반드시 채운다.** LLM 실패 시 주의사항이 통째로 사라지면 "AI가 판정한 게 아니다"라는 장치가 없어진다.
- 주의사항의 재료는 이미 엔티티에 있다 — 수용 인원 여유, `conditions`(이용 조건), `noiseAllowed`·`messAllowed`, `hourlyFee`. **새 필드를 만들지 마라.**
- **후보 0건**: 명세 권장안은 "조건 수정 안내"다. 지금은 `SpaceMatchResponse.emptyList()`라 안내를 실을 자리가 없다. 권장 = 응답을 `matched`·`suggestions`를 가진 결과 객체로 감싼다. **`NO_MATCHING_SPACE`(404)는 쓰지 않기를 권한다** — 후보 0건은 오류가 아니라 정상 결과이고, 404로 만들면 프론트가 에러 분기를 따로 짠다. `(미확정 — 결정 필요)` · **결정 시 `POST /api/ai/match`는 계약 변경이므로 front 담당에게 알린다.**
- 파일은 `ai/**`뿐이다.

### P1-3. AI 기획 가이드 보완 질문 (기능명세 2.2 · 2.2.1)

"운영 조건이 부족하면 정확한 추천이 어렵다"가 이 기능의 존재 이유인데, A-01은 **무엇이 비었는지 알려주지 않는다.**

- `RequirementResponse`에 `missingFields`와 질문 목록을 추가한다. **추가만 하므로 기존 화면은 그대로 동작한다.**
- **LLM을 다시 부르지 않는다.** 추출 결과의 빈 값 판정으로 1차 충족한다 — `region`이 공백, `headcount <= 0`, `requiredFacilities`가 빈 목록.
- **`AnalyzedRequirement`(LLM 구조화 출력 스키마)에 질문 필드를 넣지 마라.** 추출과 진단을 한 호출에 섞으면 추출 품질이 흔들리고, 빈 값 판정은 서버가 확정적으로 할 수 있다.
- 질문 문구는 **서버 상수 한 곳**에 둔다. 판정 근거가 없는 항목(예: 소요시간 — 추출 스키마에 없다)은 질문 목록에 넣지 않는다. 없는 필드를 물으면 답을 받아도 채울 자리가 없다.

### P1 공통

- **응답은 추가만 한다.** 기존 필드를 지우거나 의미를 바꾸지 않는다 — 계약을 깨는 변경은 이슈 #53(주소 마스킹)으로 이미 한 번 냈고, 그때마다 front 공지가 필요하다.
- 이슈 먼저 발급 → `yonggyu/backend`에서 분기. 브랜치는 P1-1이 `yonggyu/feat/citizen-discovery`, P1-2+P1-3이 `yonggyu/feat/ai-guide-quality`.
- 기능명세를 인용할 때는 **목차 번호만** 쓴다(위 참조 규칙).

## 📣 프론트 영향 공지 (필수)

**백엔드 계약이 바뀌면 프론트가 깨진다. 그래서 계약 변경은 작업 전과 작업 후 두 번, 사용자에게 먼저 알린다.**
에이전트는 사용자에게 보고까지만 한다 — **프론트 담당에게 직접 연락하거나 외부 채널에 공지하지 않는다.** 전달은 사용자가 한다.

### 언제 알리나 (아래 중 하나라도 해당하면 "공지 필요")

| 구분 | 예 |
|---|---|
| 새 엔드포인트 | `POST /api/ai/match` 신설 |
| 경로·메서드 변경 | `/api/activities/{id}/join` → `/api/participations` |
| 요청 계약 변경 | 필드 추가·삭제·이름 변경·**필수화**·타입 변경, 쿼리 파라미터 추가 |
| 응답 계약 변경 | 필드 **삭제**·이름 변경·타입 변경·**의미 변경**(예: 주소 마스킹), 새 필드 추가 |
| 에러 계약 변경 | 새 `ErrorCode`, HTTP 상태 코드 변경, 정상 응답 → 에러로 전환 |
| 인증·인가 변경 | 열람 범위, 역할 제한, 비로그인 허용 여부 |
| 구조적 변경 | 상태 전이 규칙, 자동 공개(S-01) 같은 플로우 변경, 목록 필터·정렬 규약 |

값만 바뀌는 내부 리팩터링·성능 개선·테스트 추가는 공지 대상이 아니다.

### 어떻게 알리나

- **작업 전 (착수 보고).** 코드를 건드리기 전에 한 줄 판정 — `프론트 공지 필요 / 불필요` — 과 **바뀔 계약의 요약**을 사용자에게 말한다.
  깨는 변경(필드 삭제·의미 변경·필수화)이 포함되면 **사용자 확인을 받고 착수한다.** 추가만 하는 변경은 알리고 그대로 진행한다.
- **작업 후 (완료 보고).** 실제로 나간 계약을 **프론트에 그대로 전달할 수 있는 형태**로 정리한다.
  - 메서드 + 경로, 요청·응답 **예시 JSON**(`ApiResponse` 래핑 포함), 추가/변경/삭제 표시
  - 새 에러코드와 HTTP 상태
  - **언제부터 유효한지** — 지금은 CD 파이프라인 배포이므로 "머지 시점"이 아니라 **"배포 완료 시점"** 기준으로 적는다
- PR 본문에도 같은 요약을 남긴다. PR만 보고도 프론트가 대응할 수 있어야 한다.

> **원칙은 변하지 않는다 — 응답은 추가만 한다.** 필드를 지우거나 의미를 바꾸는 변경은 이슈 #53(주소 마스킹)처럼 별도 이슈로 내고 공지한다.

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
- **패키지 구조**: `{도메인}/controller`, `{도메인}/service`, `{도메인}/repository`, `{도메인}/dto` — 현재 도메인: `auth`, `activity`, `space`, `ai`, `common`, `domain`(엔티티).
- **엔티티**: setter 금지, 의미 있는 도메인 메서드로 상태 변경 (예: `request.approve()`). 검증 로직은 복사하지 말고 한 곳으로 뽑아 공유한다 (예: 시간 순서 검증은 개설·수정이 함께 쓴다).
- **Swagger**: `@SecurityScheme(type = HTTP, scheme = "basic")`으로 Authorize 버튼 사용.
- 기술 스택 주의: Spring Boot 4.1 / Spring Security 7(람다 DSL만, `.and()` 체이닝 불가) / Jackson 3(`tools.jackson` 패키지).
- **테스트 작성 필수.** 대상은 `domain`/`common`/`auth`/`activity`/`space`/`ai` 전 레이어, **현재 존재하는 모든 엔드포인트**(신규만이 아니라 기존 것도 소급 적용)와 **객체가 생성되는 모든 지점**(엔티티 생성, 상태 변경 도메인 메서드, DTO 변환). 강제 규약과 금지 사항은 **[🧪 테스트 강제 규약](#-테스트-강제-규약-cd-파이프라인-게이트)** 을 따른다. Swagger 수동 확인은 보조 수단일 뿐 테스트를 대체하지 않는다.

## API 경로 규약

- 인증: `/api/auth/...`
- 일반 사용자·예술가: `/api/activities/...`, `/api/me/...`
- 공간 제공자: `/api/spaces/...`, `/api/host/...`
- AI: `/api/ai/...`

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
./gradlew test                # push 전 필수 테스트 (CI가 같은 명령을 돌린다 — 여기서 실패하면 배포도 못 나간다, DB 불필요)

curl -u myid:mypw http://localhost:8080/api/auth/me   # 인증 확인
```

## CI (GitHub Actions)

`.github/workflows/ci.yml` (이슈 #36, PR #37) — `main`/`dev`/`backend`/`yonggyu/backend` 대상 PR과 push마다 `compileJava` + `test`를 자동 실행하고 테스트 리포트를 아티팩트로 올린다. **CI가 빨간 PR은 머지하지 않는다.** 프론트엔드 job과 배포(CD) 워크플로는 `DEPLOYMENT.md` Part 1에 계획만 있고 아직 없다.

## 🧪 테스트 강제 규약 (CD 파이프라인 게이트)

**이제 배포는 CD 파이프라인이 한다. 테스트가 빨간 순간 배포가 막히거나, 더 나쁘게는 검증 안 된 코드가 나간다.**
`.github/workflows/ci.yml`이 PR·푸시마다 `compileJava` → `test`를 돌린다. **로컬에서 초록을 확인하지 않은 코드는 push하지 않는다.**

> ⚠️ 저장소의 워크플로는 지금 `ci.yml`(compile + test) 하나뿐이고, **CD 워크플로는 `DEPLOYMENT.md` Part 1에 계획만 있다.**
> 실제 배포 잡이 생기면 트리거 브랜치를 여기에 적는다. 어느 쪽이든 **테스트 통과가 배포의 선행 조건**이라는 규약은 그대로다.

### 커밋 단위 강제 규약

**프로덕션 코드가 바뀐 커밋은 테스트 코드를 동반한다.** 테스트 없는 프로덕션 커밋은 만들지 마라
(예외: 문서·주석·빌드 설정만 바뀐 커밋).

| 무엇을 바꿨나 | 반드시 있어야 하는 테스트 | 최소 케이스 |
|---|---|---|
| 새 엔드포인트 / 시그니처 변경 | `@WebMvcTest` 컨트롤러 테스트 | 성공 1 + 검증 실패(400) 1 + 권한 거부(401/403) 1 |
| 새 서비스 메서드 / 분기 추가 | Mockito 서비스 테스트 | 정상 경로 1 + `BusinessException` 경로 1(**`ErrorCode`까지 단언**) |
| 새 쿼리 / `search` 조건 추가 | `@RepositoryTest` | 걸리는 케이스 + **안 걸리는 케이스** + 경계값(날짜 당일 포함 등) |
| 엔티티 상태 변경 메서드 | 순수 JUnit | 전이 성공 + 허용되지 않는 상태에서 거부 |
| DTO 변환·계산 로직 | DTO 테스트 | 계산 결과 + 하한/상한 같은 경계(예: `remainingCapacity` 0 하한) |
| 인가 범위 변경 | `@IntegrationTest` 인가 매트릭스(`SecurityConfigTest`) | 역할별 허용/거부 전부 |
| 버그 수정 | **재현 테스트를 먼저 추가해 빨간 것을 확인한 뒤** 고친다 | 재현 1 + 회귀 방지 단언 |
| 계약 회귀(공지한 정책) | 정책이 깨지면 실패하는 테스트 | 예: `SpaceInfo`에 주소 필드가 **없음** |

### 금지 (하나라도 하면 그 작업은 실패다)

- **`@Disabled`/주석 처리로 테스트를 끄고 넘어가지 마라.** 못 고치면 끄지 말고 **멈춰서 사용자에게 보고**한다.
- **실패하는 테스트를 삭제하거나 단언을 약화시켜 초록을 만들지 마라.** 테스트가 맞고 코드가 틀린 경우가 대부분이다.
- **테스트를 통과시키려고 프로덕션 검증을 지우거나 예외를 삼키지 마라.**
- **단언 없는 테스트 금지.** "예외 없이 실행됨"만 확인하는 테스트는 통과로 치지 않는다.
- **DB·도커가 필요한 테스트 금지.** 컨텍스트가 필요하면 `@RepositoryTest`·`@IntegrationTest`의 인메모리 H2를 쓴다.
- **현재 시각·랜덤에 의존하는 단언 금지.** 날짜는 테스트 안에서 고정값으로 만든다.
- **`ANTHROPIC_API_KEY` 같은 외부 자격 증명이 있어야 도는 테스트 금지.** AI 경로는 SDK 클라이언트를 목으로 대체한다 (CI에는 키가 없다).

### 실행과 보고

- 커밋 전 `./gradlew compileJava`, **push 전 `./gradlew test`**. 둘 다 초록이어야 push한다.
- 결과는 **실제 Gradle 출력을 근거로** 보고한다. 실패했으면 실패했다고 출력과 함께 말하고, 안 돌렸으면 **안 돌렸다고 말한다.** 추측으로 "통과했습니다"라고 쓰지 마라.
- CI가 빨간 채로 PR을 두지 않는다. CI 실패는 **다음 작업보다 먼저** 고친다.
- 테스트 리포트는 CI 아티팩트 `backend-test-report`(`build/reports/tests/test`)에서 받는다.

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

5. **품질 게이트.** PR 올리기 전 `./gradlew compileJava && ./gradlew test` 통과 필수. CI가 같은 것을 다시 검증한다. **CD 파이프라인 배포이므로 빨간 브랜치는 곧 막힌 배포다** — 상세는 [🧪 테스트 강제 규약](#-테스트-강제-규약-cd-파이프라인-게이트).

   **PR 본문에는 프론트 영향 요약을 반드시 넣는다** — 계약 변경이 없으면 "프론트 영향 없음" 한 줄이라도 적는다([📣 프론트 영향 공지](#-프론트-영향-공지-필수)).

6. **통합·승격 PR.** 에이전트가 임의로 `backend`·`dev`·`main`에 머지·푸시하지 않는다.

## 현재 상태 (2026-08-18 기준)

- **`dev`가 배포 후보 브랜치다.** PR #55(`backend → dev`)로 백엔드 최신(#44 가격 표시, #47 슬롯 수정·일정 불일치, #49 활동 수정·삭제, #53 정보 노출 차단)이 승격됐고, PR #43으로 프론트 16화면이 들어와 있다. 열린 백엔드 PR은 없다.
- **프론트 기준 코드베이스는 PR #43이다.** 구 `X-Guest-Id` API 레이어를 삭제하고 HTTP Basic으로 전면 교체했다. 결제(`POST /api/activities/{id}/payment`, `GET .../settlement`)·구독(`/api/subscriptions`) 화면은 **UI만** — 해당 백엔드 API는 미구현이며, 만들 때는 루트 `CLAUDE.md`의 API 갭 표와 `frontend/docs/SCREEN_API_MAPPING.md`를 계약 기준으로 삼는다.
- **배포 준비 중.** 서버는 제공받았고, `DEPLOYMENT.md` Part 1 산출물(Dockerfile·`.dockerignore`·`application-prod.yml`·`deploy/`·CD 워크플로)은 아직 만들지 않았다. 프론트·API를 **단일 오리진**(Caddy 리버스 프록시)으로 배포한다.
  - 배포 전 처리 목록: 웹 이미지 업로드 FormData 플랫폼 분기, `app.json` `web.output` → `single`, 웹 같은 오리진 base URL, `platform.fee.matching` 설정, OpenAI 키 로테이션.
- 다음 기능 착수 후보는 **시민 탐색**(지역·일정 필터 + 확정 공간 + 잔여 인원, 기능명세 4.1)이다.

## 알려진 제약 / 같이 처리할 것

- **활동 수정·삭제는 `DRAFT`/`REJECTED`에서만 가능하다 (설계 확정).** `PENDING`은 심사 중, `PUBLISHED`는 참여자가 있어 손대지 않는다. 공개된 활동의 수정·취소는 취소·환불 정책이 정해진 뒤 별도 이슈로 다룬다 `(미확정 — 결정 필요)`.
- **공간 주소를 승인된 개최 요청의 예술가에게 공개할지 미정** `(미확정 — 결정 필요)`. 기능명세 6.1 `rules`는 "시민과 예술가에게 동 단위까지"라고만 쓴다. **결정 전까지는 소유 HOST 본인에게만** 주소 전문을 준다.
- **`AppUser` 등 테이블 생성 방식은 로컬 `application.yml`의 `ddl-auto`에 달렸다.** 이 파일은 저장소에 없으므로(gitignore) 직접 확인해야 한다. **prod에는 `create` 절대 금지** (`DEPLOYMENT.md`).
- actuator 의존성이 없어 `/actuator/health`가 없다 — 배포 헬스체크는 공개 `GET /api/activities`로 대체한다.
- 미해결 버그 이슈: #9(모집 정원이 승인 공간 수용인원과 무관), #8·#30(잘못된 enum·파라미터가 400 대신 500), #11(과거 날짜로 활동 개설 가능 — 개설 DTO에 `@FutureOrPresent` 누락, 수정 DTO에는 있음). 데모를 막지는 않는다.
