# Borrow(아트민) — 팀 공용 에이전트 가이드 (모노레포)

천안 유휴공간 대여 해커톤 MVP. 카페·식당 등 **다른 용도로 운영되다 비는 시간**을 취미 모임·원데이클래스 공간으로 매칭한다. 서비스명은 "아트민(artmin)"으로 개명 예정이다.

> 초기 기획(FastAPI AI 마이크로서비스, 사장/학생 2개 앱, 게스트 UUID 인증)은 **전부 폐기됐다.** 아래가 현재 구조다.

## 저장소 구성

| 경로 | 내용 | 상세 문서 |
|---|---|---|
| `backend/` | Spring Boot 4.1 + MySQL. 인증·CRUD·AI 매칭 전부 담당 (별도 AI 서비스 없음) | `backend/CLAUDE.md` |
| `frontend/` | Expo SDK 54 (React Native + Expo Router). 회원 유형별 화면을 한 앱에 통합 | `frontend/BUILD.md` |
| `DEPLOYMENT.md` | CI/CD·가비아 클라우드 배포 계획·런북 | — |

## 서비스 핵심 플로우

취미 모임 개설(일반 회원) → AI 공간 매칭 → 개최 요청 전송 → 공간 제공자 승인 → 활동 자동 공개 → 다른 회원 참여 신청

## 인증 (전 스택 공통)

**ID/PW 로그인 + HTTP Basic.** 초기 계획의 `X-Guest-Id` 게스트 UUID는 폐기됐다 (백엔드 이슈 #26, 프론트 PR #43).

- 매 요청에 `Authorization: Basic base64(loginId:password)`. 세션·토큰 없음(STATELESS).
- **별도 로그인 API가 없다** — `POST /api/auth/signup`으로 가입하고, `GET /api/auth/me`가 200이면 로그인 성공으로 판정한다. 프론트는 이 방식으로 자격증명을 검증해 AsyncStorage에 저장한다.
- 역할은 가입 시 하나만 선택: `MEMBER`(모임 개설·참여) / `HOST`(공간·개최요청 관리) / `ARTIST`(MEMBER + 원데이클래스 개설).
- **`ADMIN`은 가입으로 만들 수 없다** (기능명세 7 관리자 콘솔, 이슈 #86). signup이 400으로 거부하고, 계정은 `ADMIN_LOGIN_ID`·`ADMIN_PASSWORD` 환경변수를 읽는 시드 러너가 만든다. 관리자는 `/api/admin/**`와 프론트 `(admin)` 라우트 그룹만 쓴다.
- **자격증명이 매 요청 평문으로 흐른다**(base64는 암호화가 아니다). 외부 배포에서는 HTTPS가 사실상 필수 전제이고, 평문 HTTP 구간에서는 실제 개인정보를 넣고 시연하지 않는다.

## 프론트엔드 — 기준: PR #43 (`dev`에 머지됨)

Figma 16화면 구현 + 백엔드 REST API 매핑이 코드베이스 기준이다. 화면별 API 매핑은 `frontend/docs/SCREEN_API_MAPPING.md` 참고.

- `src/lib/api/` — HTTP Basic 클라이언트(`client.ts`), 백엔드 DTO 1:1 타입(`types.ts`), 8개 네임스페이스(`endpoints.ts`): `authApi`·`activityApi`·`meApi`·`spaceApi`·`hostApi`·`aiApi`·`uploadApi`·`adminApi`
- `src/lib/auth.tsx` — `AuthProvider`: AsyncStorage 영속 자격증명, 앱 재시작 시 자동 복구, 인증 상태 기반 리다이렉트
- 라우트: `(auth)/login`, `(user)/`(홈·활동·내활동·개설), `(provider)/`(홈·요청·공간), `(admin)/`(홈·회원·프로그램·공간 — ADMIN 전용, 레이아웃 가드), `admin-form/`(관리자 데이터 생성·수정 폼 3종), `activity/[id]`, `request/[id]`, `payment/[id]/`, `subscription/`
- **구 API 레이어(`src/api/`, `src/lib/guest.ts`, `X-Guest-Id`)는 삭제됐다. 부활시키지 마라.**

### 결제·구독 화면은 UI만 — 백엔드 API 갭

| 기능 | 필요 API | 상태 |
|---|---|---|
| 결제 처리 | `POST /api/activities/{id}/payment` | 미구현 |
| 정산 미리보기 | `GET /api/activities/{id}/settlement` | 미구현 |
| 구독 가입/조회/해지 | `POST`·`GET`·`DELETE /api/subscriptions(/me)` | 미구현 |

해당 화면은 mock/static 데이터를 쓴다. 백엔드에 이 API를 만들 때는 위 경로와 `SCREEN_API_MAPPING.md`를 계약 기준으로 삼는다.

> 매칭 이용료는 백엔드가 개최 요청 응답에 이미 내려준다(기능명세 5.1). 화면에 금액을 하드코딩하지 말고 응답의 `PriceBreakdown`을 쓴다.

## 브랜치 전략 (저장소 공통)

```
<이름>/feat/*  →  <이름>/backend  →  backend  →  dev  →  main
   (작업)         (개인 작업 공간)   (팀 공용 통합)  (통합 검증)  (릴리스)
```

- 백엔드: `<이름>/backend`(예: `yonggyu/backend`, `kang/backend`)가 개인 작업 공간. `backend`·`dev`·`main`에 직접 푸시하지 않고, 통합·승격 머지는 팀 합의 시점에만 한다.
- 프론트엔드: `front` 브랜치에서 작업하고 `dev`를 base로 PR을 연다 (예: PR #43).
- 모든 작업은 **이슈 발급 → 브랜치 → `[#이슈번호]` 커밋 → PR** 순서. 상세 절차는 `backend/CLAUDE.md`의 Git 규칙 참고.
- **기능명세를 가리킬 때는 목차 번호만 쓴다** (`기능명세 4.1`). 명세 도구의 내부 식별자(`F-`·`R-`·`S-` 6자 코드)를 문서에 남기지 마라. 목차 표는 `backend/CLAUDE.md`에 있다.

## CI

`.github/workflows/ci.yml` — job 2개가 돈다. **CI가 빨간 PR은 머지하지 않는다.**

- `backend (compile + test)` — `compileJava` + `test`
- `frontend (typecheck + lint + web build)` — `tsc --noEmit` + `expo lint` + `expo export --platform web`

대상: PR은 `main`/`dev`/`backend`, push는 여기에 `yonggyu/backend`·`kang/backend`·`front`가 더해진다.
배포(CD)는 `DEPLOYMENT.md` Part 1에 계획만 있고 아직 없다.

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

## 배포 현황 (2026-08-20)

**운영 중이다.** https://artmin.duckdns.org — `main` push마다 `.github/workflows/deploy.yml`이 백엔드 이미지와 프론트 웹 번들을 빌드해 자동 배포한다. `dev`가 배포 후보 브랜치다.

### 업로드 저장소 — 되돌리면 안 되는 두 가지

업로드 이미지는 루트 디스크가 아니라 **블록 볼륨**(`/dev/vdb` → `/mnt/borrow-data/uploads`)에 둔다. 이미지가 루트를 채우면 MySQL과 도커가 함께 죽는다.

1. **호스트 업로드 디렉터리는 컨테이너 유저 uid `999` 소유여야 한다.** `backend/Dockerfile`이 `chown -R app:app /app`을 하지만 compose의 bind mount가 그 위를 덮어써서 무효가 된다. 안 맞추면 업로드가 전부 `Permission denied`로 실패하고 화면에는 "업로드 실패"만 뜬다.
2. **fstab에 `nofail`을 반드시 넣는다.** 빠뜨리면 볼륨이 분리됐을 때 부팅이 emergency shell에서 멈춰 서버에 접속조차 못 한다.

CD는 배포마다 `docker-compose.prod.yml`·`Caddyfile`을 리포지토리 버전으로 덮어쓰지만 서버 `.env`는 건드리지 않는다. 그래서 경로는 **compose가 아니라 `.env`의 `UPLOAD_HOST_DIR`로** 지정한다 — compose를 고치면 다음 배포에 지워진다. 전체 절차는 로컬 `DEPLOYMENT.md`(gitignore) "3-A. 업로드 저장소" 참고.

**배포 전 처리 목록**

- 프론트 웹 대응 — ~~웹 이미지 업로드 FormData 플랫폼 분기~~(이슈 #102 에서 해결), `app.json` `web.output`을 `single`로, 웹은 같은 오리진(`''`)을 쓰도록 base URL 처리
- `platform.fee.matching` 설정값 — **비어 있으면 앱이 기동에 실패한다.** primitive `int` 라 빈 환경변수가 바인딩 오류를 내고 크래시 루프에 빠진다(0원 표시가 아니다). 2026-08-20 운영에서 실제 발생
- OpenAI 키 로테이션 후 `.env`로만 관리 (`${VAR:실제값}` 기본값 패턴 금지)
- 로컬 프로덕션 리허설(전체 플로우 완주)을 거친 뒤 서버에 올린다

## 보안 (요약)

- `application.yml` 등 `*.yml`은 gitignore — 시크릿은 `.env`로만. **`${VAR:실제값}` 기본값 패턴 금지.**
- 비밀번호는 BCrypt 해시로만 저장. 로그에 `Authorization` 헤더·비밀번호를 찍지 마라.
- 비공개 정보는 서비스·DTO 계층에서 막는다 — 비공개 활동 상세는 개설자 본인에게만, 공간 주소 전문은 소유 HOST에게만.

전체 규칙은 `backend/CLAUDE.md`의 🔐 보안 규칙 참고.
