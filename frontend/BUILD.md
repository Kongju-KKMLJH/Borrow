# 아트민 (구 Borrow) — 로컬 실행 / 빌드 런북

> 새 세션에서 이 파일만 보고 개발 환경(백엔드 + MySQL + Expo)을 다시 띄울 수 있게 정리한 문서.
> 앱 이름은 **Borrow → 아트민**으로 변경됨(표시명/로고). slug·scheme = `artmin`.

## 아키텍처

```
Expo(아트민 앱)  ──►  Spring(:8080, CRUD·매칭·업로드·DB)  ──►  AI(FastAPI, 선택)
                         └──►  MySQL(:3306, docker)
```
- 앱은 **Spring만** 호출. 인증은 로그인 없이 프론트가 만든 UUID를 `X-Guest-Id` 헤더로 전송.

## 디렉토리 / 브랜치 구조 (모노레포)

- 리포 루트: `/Users/taehyunjeon/taehyun/dev/project/Borrow` (`.git` 하나, `frontend/`·`backend/` 디렉토리)
- 프론트 작업 브랜치: `feat/frontend` (워킹트리 = 리포 루트)
- **백엔드 최종 코드: `origin/backend`** (현재 `99a54dc`, image-upload 머지 포함)
  - `feat/frontend` 트리의 `backend/`는 **스켈레톤**(컨트롤러 없음) — 실제로 돌리는 건 이거 아님!
- 백엔드 실행 워크트리: **`/Users/taehyunjeon/taehyun/dev/project/Borrow-backend-preview`** (detached, `origin/backend`로 checkout해서 사용)
  - `application.yml` 및 `.env`는 **`.gitignore` 처리됨**(민감정보). git엔 없고 이 워크트리에 로컬로만 존재.

## 1) MySQL (docker, 최초 1회)

```bash
cd /Users/taehyunjeon/taehyun/dev/project/Borrow-backend-preview/backend
docker compose up -d          # 컨테이너명 borrow-mysql, :3306, db/user/pw = borrow / borrow / borrow1234
docker ps | grep borrow-mysql # Up 확인
```
데이터는 named volume(`mysql-data`)에 영속 — 백엔드 재시작해도 유지됨.

## 2) 백엔드 (Spring, :8080)

```bash
cd /Users/taehyunjeon/taehyun/dev/project/Borrow-backend-preview
git fetch origin backend && git checkout 99a54dc   # 최신 백엔드 코드로
cd backend
# 권장: jar 빌드 후 detached 실행 (아래 "실행 주의" 참고)
./gradlew bootJar --console=plain
nohup java -jar build/libs/borrow-0.0.1-SNAPSHOT.jar > /tmp/artmin-backend.log 2>&1 &
# (간단 실행이면: ./gradlew bootRun — 단 gradle 데몬 종료 시 함께 죽을 수 있음)
```
- http://localhost:8080, `0.0.0.0` 바인딩(LAN 노출).
- 구 인스턴스가 8080 점유 중이면 먼저 종료: `lsof -ti:8080 | xargs kill -9`
- **실행 주의**: `gradlew bootRun`은 gradle 빌드 데몬 수명에 묶여, 데몬이 stop되면 앱도 죽고 8080에 500만 내는 좀비가 남을 수 있음. **`bootJar` + `java -jar`(nohup detached)** 가 안정적 — gradle과 무관하게 계속 실행됨. 로그: `/tmp/artmin-backend.log`.
- `application.yml`(gitignore, 로컬): datasource(borrow/borrow1234@localhost:3306) + `ddl-auto: update`.
  신규 이미지 필드는 `@ElementCollection` → `*_image_urls` 테이블 자동 추가(기존 데이터 안전).
- **AI**: `AI_PROVIDER`/API 키 env가 있어야 LLM 매칭. 없으면 `analyze`는 500, `match`는 **규칙 기반 폴백**으로 동작(데모 지장 없음).
- 멀티파트 한도가 필요하면 application.yml `spring.servlet.multipart.max-file-size: 10MB / max-request-size: 60MB`.

### 확인
```bash
curl -s http://localhost:8080/v3/api-docs | grep -o '/api/uploads'   # 나오면 신규(이미지) 빌드
curl -s http://localhost:8080/api/spaces -H 'X-Guest-Id: t' | head -c 200
```

## 3) 프론트 (Expo, :8081)

```bash
cd /Users/taehyunjeon/taehyun/dev/project/Borrow/frontend
npx expo start --lan --clear
```
- **SDK 54 고정**(Expo Go 제약). 필요한 네이티브 모듈(expo-image-picker/manipulator)은 Expo Go에 기본 포함 → 커스텀 빌드 불필요.
- API 주소는 `.env.local`의 `EXPO_PUBLIC_API_BASE_URL`로 주입(gitignore됨). 앱은 여기에 맞춰 백엔드/이미지 URL을 구성.

### `.env.local` (실기기용 — 맥 LAN IP)
```
EXPO_PUBLIC_API_BASE_URL=http://<맥 LAN IP>:8080
```
- **실기기(Expo Go)에서 `localhost`는 폰 자신** → 반드시 맥 LAN IP 사용.
- LAN IP 확인: `ipconfig getifaddr en0` (유선 `en1`). 네트워크 바뀌면 이 값 갱신 후 Metro 재시작.
- 폰 접속: Expo Go에서 `exp://<맥 LAN IP>:8081` 직접 입력, 또는 QR. 폰·맥 **같은 Wi-Fi** 필수(게스트/AP 격리 주의).
- 백엔드·Metro 모두 LAN 접속 확인: `curl http://<LAN IP>:8080/api/activities -H 'X-Guest-Id: t'`

> 백그라운드 실행 시 Expo가 QR/`exp://`를 TTY에만 그려서 안 보일 수 있음 → URL은 `exp://<LAN IP>:8081`로 확정.
> standalone(비 Expo Go) 빌드에서 http 접속 시 cleartext 예외 필요: `expo-build-properties`(README 참고). Expo Go 개발 중엔 대체로 통과.

## 이미지 업로드 계약 (프론트 구현 완료분)

**2단계**: 먼저 업로드해 URL을 받고, 그 배열을 등록에 담는다. 모든 이미지 필드는 **`imageUrls: string[]`**(배열).

1. **업로드** `POST /api/uploads` (multipart/form-data, 파트명 **`files`**, 1장 이상)
   → `{ success, data: { urls: ["/files/a.jpg", ...] } }` (상대경로)
2. **등록** `POST /api/spaces` · `POST /api/activities` (JSON)에 `"imageUrls": ["/files/a.jpg", ...]`
3. **조회/표시**: 응답 `imageUrls`는 상대경로 → 앞에 base URL 부착: `http://<맥 IP>:8080/files/a.jpg`

### 프론트 구현 위치
- `src/api/uploads.ts` — `uploadImages(uris)`: expo-image-manipulator로 압축(width 1280·q0.8) 후 multipart POST
- `src/api/client.ts` — `imageUri(path)`: 상대경로에 `EXPO_PUBLIC_API_BASE_URL` 부착
- `src/components/image-upload-field.tsx` — 갤러리 다중선택 + 촬영 + 썸네일/삭제(최대 5장)
- 적용: `src/app/(user)/create.tsx`(활동 만들기 Step2), `src/app/(provider)/space.tsx`(공간 관리)
- 표시: `activity-card`, `space-match-card`, `(provider)/index.tsx` — `imageUrls[0]` + `imageUri()`, 없으면 플레이스홀더
- 타입: `src/data/types.ts` — Space/SpacePayload/SpaceMatch/ActivitySummary/ActivityCreatePayload 모두 `imageUrls`

## 현재 알려진 상태 / 주의

- **AI analyze 500 / match 규칙 기반**: `AiConfig`는 provider=anthropic(claude-opus-4-8)로 뜨지만 analyze는 여전히 500(API 키 미설정/무효 추정). match는 규칙 기반 폴백. 프론트는 폴백 처리(플로우 지장 없음).
- **공간 매칭은 슬롯 기반**: 시드 공간 "공주대"는 월~금·일 14:00–17:00만 열림(토요일 없음) → 그 외 날짜는 추천 0건.
- 활동 참여 목록 정렬: `임박순`(가까운 활동일 먼저).

## 흔한 실패 (README 발췌)

| 증상 | 원인 |
|---|---|
| 폰에서 서버 접속 안 됨 | 다른 Wi-Fi / AP 격리 / 맥 방화벽 8080 차단 |
| 업로드는 되는데 이미지 안 보임 | 상대경로에 base URL 미부착 / cleartext(HTTP) 차단 |
| 413 Payload Too Large | 원본 대용량 — 앱 압축(적용됨) 또는 멀티파트 한도 상향 |
| 활동 만들기 제출 400 | 백엔드가 구 빌드(`imageUrls` 미지원) — 신규 코드로 재시작 필요 |
