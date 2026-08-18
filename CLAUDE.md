# Borrow — 팀 공용 에이전트 가이드 (모노레포)

천안 유휴공간 대여 해커톤 MVP. 카페·식당 등 **다른 용도로 운영되다 비는 시간**을 취미 모임·원데이클래스 공간으로 매칭한다. 서비스명은 "아트민(artmin)"으로 개명 예정.

> 초기 기획(FastAPI AI 마이크로서비스, 사장/학생 2개 앱, 게스트 UUID 인증)은 **전부 폐기됐다.** 아래가 현재 구조다.

## 저장소 구성

| 경로 | 내용 | 상세 문서 |
|---|---|---|
| `backend/` | Spring Boot 4.1 + MySQL. 인증·CRUD·AI 매칭 전부 담당 (별도 AI 서비스 없음) | `backend/CLAUDE.md` |
| `frontend/` | Expo SDK 54 (React Native + Expo Router). 회원 유형별 화면을 한 앱에 통합 | `frontend/BUILD.md` |
| `DEPLOYMENT.md` | CI/CD·가비아 클라우드 배포 계획 (계획 단계) | — |

## 서비스 핵심 플로우

취미 모임 개설(일반 회원) → AI 공간 매칭 → 개최 요청 전송 → 공간 제공자 승인 → 활동 자동 공개 → 다른 회원 참여 신청

## 인증 (전 스택 공통)

**ID/PW 로그인 + HTTP Basic.** 초기 계획의 `X-Guest-Id` 게스트 UUID는 폐기됐다 (백엔드 PR #28·#38, 프론트 PR #43).

- 매 요청 `Authorization: Basic base64(loginId:password)`. 세션·토큰 없음(STATELESS).
- **별도 로그인 API 없음** — `POST /api/auth/signup`으로 가입, `GET /api/auth/me` 200이면 로그인 성공으로 판정.
- 역할은 가입 시 하나만 선택: `MEMBER`(모임 개설·참여) / `HOST`(공간·개최요청 관리) / `ARTIST`(MEMBER + 원데이클래스 개설).

## 프론트엔드 현황 — 기준: PR #43 (`front` 브랜치)

Figma 16화면 구현 + 백엔드 REST API 매핑이 코드베이스 기준이다. 화면별 API 매핑은 `frontend/docs/SCREEN_API_MAPPING.md` 참고.

- `src/lib/api/` — HTTP Basic 클라이언트(`client.ts`), 백엔드 DTO 1:1 타입(`types.ts`), 7개 네임스페이스 30개 엔드포인트(`endpoints.ts`): `authApi`·`activityApi`·`meApi`·`spaceApi`·`hostApi`·`aiApi`·`uploadApi`
- `src/lib/auth.tsx` — `AuthProvider`: AsyncStorage 영속 자격증명, 앱 재시작 시 자동 복구, 인증 상태 기반 리다이렉트
- 라우트: `(auth)/login`, `(user)/`(홈·활동·내활동·개설), `(provider)/`(홈·요청·공간), `activity/[id]`, `request/[id]`, `payment/[id]/`, `subscription/`
- **구 API 레이어(`src/api/`, `src/lib/guest.ts`, X-Guest-Id)는 삭제됐다. 부활시키지 마라.**

### 결제·구독 화면은 UI만 — 백엔드 API 갭

| 기능 | 필요 API | 상태 |
|---|---|---|
| 결제 처리 | `POST /api/activities/{id}/payment` | 미구현 |
| 정산 미리보기 | `GET /api/activities/{id}/settlement` | 미구현 |
| 구독 가입/조회/해지 | `POST·GET·DELETE /api/subscriptions(/me)` | 미구현 |

해당 화면은 mock/static 데이터를 쓴다. 백엔드에 이 API를 만들 때는 위 경로를 기준으로 프론트와 계약을 맞춘다.

## 브랜치 전략 (저장소 공통)

```
<이름>/feat/*  →  <이름>/backend  →  backend  →  dev  →  main
   (작업)         (개인 작업 공간)   (팀 공용 통합)  (통합 검증)  (릴리스)
```

- 백엔드: `<이름>/backend`(예: `yonggyu/backend`, `kang/backend`)가 개인 작업 공간. `backend`·`dev`·`main` 직접 푸시 금지, 승격 머지는 팀 합의 시점에만.
- 프론트엔드: `front` 브랜치에서 작업, base `dev`로 PR (예: PR #43).
- 모든 작업은 **이슈 발급 → 브랜치 → `[#이슈번호]` 커밋 → PR** 순서. 상세 절차는 `backend/CLAUDE.md`의 Git 규칙 참고.

## CI

`.github/workflows/ci.yml` — `main`/`dev`/`backend`/`yonggyu/backend` 대상 PR·push마다 백엔드 `compileJava` + `test` 자동 실행. **CI가 빨간 PR은 머지하지 않는다.** 프론트 job·배포(CD)는 `DEPLOYMENT.md` Part 1에 계획만 있고 아직 없다.

## 명령어

```bash
# 백엔드 (backend/)
docker compose up -d          # MySQL (최초 1회)
./gradlew bootRun             # → http://localhost:8080/swagger-ui.html
./gradlew compileJava && ./gradlew test   # PR 전 필수 (테스트는 H2, DB 불필요)

# 프론트엔드 (frontend/)
npm install
npx expo start                # 폰은 LAN IP로 접속 (frontend/BUILD.md 참고)
npx tsc --noEmit              # 타입 체크
```

## 보안 (요약)

- `application.yml` 등 `*.yml`은 gitignore — 시크릿은 `.env`로만. **`${VAR:실제값}` 기본값 패턴 금지.**
- 비밀번호는 BCrypt 해시로만 저장. 로그에 `Authorization` 헤더·비밀번호 금지.
- LAN 평문 HTTP 전제이므로 실제 개인정보를 넣고 시연하지 않는다.

전체 규칙은 `backend/CLAUDE.md`의 🔐 보안 규칙 참고.
