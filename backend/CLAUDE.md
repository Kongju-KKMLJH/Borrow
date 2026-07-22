# Borrow Backend — 팀 공용 에이전트 가이드

천안 유휴공간 대여 서비스의 Spring Boot 백엔드. 해커톤 MVP이므로 **빠르고 단순하게**, 과한 추상화 금지.

## 서비스 핵심 플로우
취미 모임 개설(게스트) → AI 공간 매칭 → 개최 요청 전송 → 공간 제공자 승인 → 활동 자동 공개(S-01) → 게스트 참여 신청

로그인 없음. 사용자 식별은 프론트가 생성한 UUID를 `X-Guest-Id` 헤더로 전달받는다.

## 용어 (기능명세 기준)

| 명세 용어 | 코드 |
|---|---|
| 활동 유형: 취미 모임 / 전문 클래스 | `ActivityType.HOBBY` / `ActivityType.CLASS` |
| 분야: 그림 / 촬영 | `ActivityField.ART` / `ActivityField.PHOTO` |
| 인증 예술가 여부 (F-01, Mock) | `Activity.hostCertified` — 시드 데이터로만 true, 인증 로직 구현 금지 |

일반 사용자가 개설하는 것은 항상 취미 모임(HOBBY). 전문 클래스(CLASS)는 시드 데이터·필터에서만 등장한다.

## ⚠️ 소유권 경계 (가장 중요한 규칙)

세 명이 각자 브랜치에서 병렬 작업 중이다. **담당 패키지 밖의 파일은 읽기만 하고 절대 수정하지 마라.**
수정이 꼭 필요하면 코드를 고치지 말고 사용자에게 알리고 멈춰라.

> **백엔드 리더 예외 (A):** A는 백엔드 리더로서 **모든 파일·영역(common/domain/ai/space/activity, build.gradle, application.yml 포함)을 수정할 권한**을 가진다. 단 A는 남의 도메인을 건드릴 때 해당 담당자와 공유하고, 도메인 엔티티(`domain/`) 구조 변경은 여전히 팀에 공지한다.
> **B, C는 이 리더 예외의 대상이 아니다.** B와 C는 위 소유권 경계를 **반드시** 준수하며, 담당 패키지 밖은 읽기만 한다.

| 패키지 | 담당 | 내용 |
|---|---|---|
| `common/` | A | 응답 포맷, 예외, 게스트 인터셉터, 설정 — **동결됨, A만 수정** |
| `domain/` | 전원 합의 | 엔티티 전체 — **동결됨, 변경은 구두 합의 후 반영** |
| `ai/` | A | A-01 활동 분석, A-02/A-03 공간 매칭·적합도, A-04 대체 추천 |
| `space/` | B | 공간 CRUD, 유휴시간, 개최요청 승인/거절, 사업자 홈 (B-01~B-11) |
| `activity/` | C | 취미 모임 개설/목록/검색/상세, 개최요청 전송/상태, 참여/취소, 내 활동 (U-01~U-14, S-01) |

- `build.gradle`, `application.yml` 변경도 A 담당. 필요하면 요청할 것.
- `HostingRequest` 엔티티는 B 소유. C는 생성(U-11)/조회(U-12) API만 얹는다.

## 🔐 보안 규칙 (필수)

**민감 정보는 절대 소스에 커밋하지 않는다.** DB 자격 증명, API 키, 시크릿 등은 코드/설정 파일에 평문으로 두지 마라.

- `application.yml`을 포함한 `*.yml` / `*.yaml`은 `.gitignore`로 커밋 금지 (`docker-compose.yml`만 예외로 추적).
- 실제 값은 루트/`backend`의 `.env`로 관리하고, `application.yml`에는 `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` 형태의 **플레이스홀더만** 둔다. 기본값(`${DB_PASSWORD:비밀번호}`)에 실제 값을 넣지 마라.
- 이미 커밋된 파일은 `.gitignore` 추가만으로 빠지지 않는다. `git rm --cached <파일>`로 추적을 끊어야 한다.
- 히스토리에 이미 노출된 자격 증명은 파일 수정과 별개로 **비밀번호/키 로테이션**이 필요하다.
- 새 설정 파일을 추가할 때 시크릿이 들어갈 여지가 있으면 반드시 `.env`로 분리한다.

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

## Git 규칙 (이슈 기반 워크플로우)

모든 기능 개발·버그 수정은 **이슈 발급 → 브랜치 분기 → 작은 단위 커밋/푸시 → PR** 순서로 진행한다.
분기/머지 대상은 항상 **`backend` 브랜치** (main 아님).

1. **이슈 먼저 발급 (필수).** 작업 착수 전 `.github/ISSUE_TEMPLATE/`의 템플릿으로 GitHub 이슈를 만든다.
   - 기능 개발 → `Feature`(feature.md) / 외부 요청 작업 → `Feature request`(feature_request.md)
   - 버그 수정 → `Bug`(bug.md) / 질문 → `Question`(question.md)
   - 템플릿의 상세·체크리스트 항목을 채운다. 발급된 **이슈 번호**를 이후 브랜치·커밋·PR에 사용한다.
   - 예: `gh issue create --template feature.md` (gh 인증 필요) 또는 GitHub 웹의 이슈 템플릿.

2. **브랜치.** `backend`에서 이슈 단위로 `feat/<기능이름>` 브랜치를 분기해 작업한다.
   - `git switch backend && git pull --rebase origin backend` 후 `git switch -c feat/<기능이름>`.
   - 작업은 항상 자기 담당 패키지 안에서만 (위 소유권 경계 표 유지).

3. **커밋.** 작은 작업 단위마다 `[#이슈번호] 커밋 메시지` 형태로 커밋하고 바로 push한다.
   - 예: `[#12] A-01 활동 분석 서비스 추가`. 한 커밋 = 한 논리 단위, 큰 덩어리로 몰아 커밋하지 말 것.

4. **PR.** 이슈 단위 작업이 모두 끝나면 `.github/PullRequestTemplate.md` 템플릿으로 PR을 생성한다.
   - 제목 `[#이슈번호] 작업내용`, base 브랜치 **`backend`**.
   - 본문의 `Closes #<이슈번호>`를 채워 머지 시 이슈가 자동으로 닫히게 한다.
   - 예: `gh pr create --base backend --title "[#12] AI 공간 매칭" --body-file .github/PullRequestTemplate.md`.

5. **품질 게이트.** PR 올리기 전 `./gradlew compileJava` 통과 필수. `backend` 브랜치는 항상 컴파일되는 상태를 유지한다.

6. `backend → main` 머지는 팀 합의 시점에만.

## AI 매칭 구현 방침 (A 참고)

- A-01: Claude API 1회 호출로 활동 설명 → 구조화 조건(분야/인원/시설). Java SDK의 structured output(`outputConfig(클래스)`) 사용, 모델은 `claude-opus-4-8`.
- A-02: LLM 아님 — JPA 쿼리 하드 필터 (지역, 수용인원, 허용분야, 슬롯 시간 겹침).
- A-03: 필터 통과 후보들을 한 번의 Claude 호출로 점수+추천이유 산출. **API 실패 시 규칙 기반 점수로 폴백 필수** (데모 안정성).
- A-04: 거절된 공간 제외 후 A-02+A-03 재실행.
