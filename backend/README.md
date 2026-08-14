# Borrow Backend

천안 유휴공간 대여 서비스의 Spring Boot 백엔드 (해커톤 MVP).
ID/PW 로그인(Spring Security + HTTP Basic). 회원 유형은 역할(Role)로 나눈다 —
`MEMBER`(일반 회원) / `HOST`(공간 제공자) / `ARTIST`(예술가).

- 스택: Spring Boot 4.1 / Java 17 / MySQL 8.4 / JPA
- API 문서(Swagger): http://localhost:8080/swagger-ui.html

## 빠른 실행

```bash
docker compose up -d       # MySQL 기동 (최초 1회)
./gradlew bootRun          # 서버 실행 (http://localhost:8080)
./gradlew compileJava      # 커밋 전 빌드 확인
```

AI 매칭 기능은 `.env`의 `AI_PROVIDER` / API 키 환경변수가 필요하다 (`application.yml` 주석 참고).

---

## 인증 (ID/PW · HTTP Basic)

매 요청에 `Authorization: Basic base64(로그인아이디:비밀번호)` 헤더를 보낸다.
별도 로그인 API는 없다 — 프론트의 "로그인 버튼"은 `GET /api/auth/me`를 호출해 200이면 성공 처리한다.

```bash
# 회원가입 (비로그인 호출)  role: MEMBER | HOST | ARTIST
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"loginId":"hong","password":"pw1234","nickname":"홍길동","role":"MEMBER"}'

# 인증 확인 / 내 정보
curl -u hong:pw1234 http://localhost:8080/api/auth/me
#  → {"success":true,"data":{"loginId":"hong","nickname":"홍길동","role":"MEMBER"},"error":null}
```

Swagger UI 우측 상단 **Authorize** 버튼에 아이디/비밀번호를 넣으면 이후 호출에 자동 적용된다.
미인증은 401 `UNAUTHORIZED`, 역할이 맞지 않으면 403 `FORBIDDEN` — 둘 다 공통 `{success, data, error}` 포맷으로 나간다.

### 역할별 권한

| 대상 | 접근 범위 |
|---|---|
| 누구나(비로그인) | `POST /api/auth/signup`, `GET /api/activities`(목록·상세), `GET /api/spaces`(목록·상세·슬롯), `/files/**`, Swagger |
| 로그인 전체(역할 무관) | 활동 참여·취소, `/api/me/**`, `/api/ai/**`, `POST /api/uploads`, `GET /api/auth/me` |
| `MEMBER`, `ARTIST` | 활동 개설, 요구조건 수정, 개최 요청 전송·상태 조회 |
| `HOST` | `/api/spaces/**` 쓰기(등록·수정·삭제·슬롯), `/api/host/**`(요청 목록·승인·거절·홈·일정) |

활동 유형과 배지는 **서버가 로그인 역할을 보고 정한다** — MEMBER 개설 → `type=HOBBY`·`hostCertified=false`,
ARTIST 개설 → `type=CLASS`·`hostCertified=true`. 개설자/참여자 닉네임도 계정 값으로 서버가 채우므로
요청 본문에 `type`·`hostCertified`·`hostNickname`·`nickname`을 보내지 않는다.

### 회원 정보는 DB에 적재된다

가입한 계정은 **`app_user` 테이블에 저장**된다. 요청 헤더에만 존재하다 사라지던
기존 게스트(UUID) 방식과 달리, 서버를 재시작해도 계정은 남는다.

| 컬럼 | 내용 |
|---|---|
| `login_id` | 로그인 아이디. **유니크** — 중복 가입은 409 `DUPLICATE_LOGIN_ID` |
| `password` | **BCrypt 해시**. 평문으로 저장하지 않고 응답 DTO에도 담지 않는다 |
| `nickname` | 활동 개설자·참여자 표시 이름의 원본 |
| `role` | `MEMBER` / `HOST` / `ARTIST` — 문자열로 저장 |

- **매 요청마다 DB를 조회해 자격증명을 검증한다**(`AppUserDetailsService`). 따라서 **DB가 떠 있지 않으면
  로그인 자체가 되지 않는다** — 비로그인 공개 조회만 응답한다.
- `loginId` 값이 **소유권 판정의 기준값**이다. `Activity.guestId`, `Participation.guestId`,
  `Space.ownerId`에 이 값이 그대로 들어가므로, 가입 후 아이디는 바꾸지 않는다.
- 테이블은 JPA `ddl-auto` 설정으로 생성된다.

> 기존 게스트(UUID) 데이터는 로그인 아이디와 값이 달라 소유권 판정이 깨진다. **DB를 비우고 시작할 것.**

---

## 이미지 업로드 (모바일 앱 MVP)

앱(Expo)에서 카메라 촬영·갤러리 선택 이미지를 **여러 장** 올리고 URL로 조회하는 구조.
파일은 서버 로컬 디스크에 저장하고, 앱은 HTTP로 그 URL을 받아 조회한다.

- 저장 위치: 서버 로컬 `uploads/` (환경변수 `UPLOAD_DIR`로 변경 가능)
- 조회 경로: `/files/**` 정적 서빙
- 실제 플랫폼 배포 시 `FileStorageService`만 S3/Cloudinary 구현으로 교체하면 앱·API 계약은 그대로.

### API 계약 — 2단계 플로우

이미지를 먼저 업로드해 URL을 받은 뒤, 그 URL 배열을 등록 요청에 담아 보낸다.

**1) 업로드**
```
POST /api/uploads          Content-Type: multipart/form-data
파트: files=<이미지 1장 이상>
→ { "success": true, "data": { "urls": ["/files/a.jpg", "/files/b.jpg"] } }
```
- 이미지 파일만 허용(`image/*`), 빈 파일 거부
- 한도: 파일당 10MB / 요청당 60MB (`application.yml`)

**2) 등록 (공간/활동)**
```
POST /api/spaces      또는   POST /api/activities        Content-Type: application/json
{ ..., "imageUrls": ["/files/a.jpg", "/files/b.jpg"] }   // 1번에서 받은 값 그대로
```

**3) 조회**
- 공간/활동 응답의 `imageUrls`는 **상대 경로**다.
- 앱은 앞에 서버 base URL을 붙여 표시한다: `http://<서버-주소>:8080/files/a.jpg`

---

## 서버 주소 / HTTP 통신

앱과 서버는 **HTTP로 통신**한다. 앱이 아는 것은 **서버 base URL 한 값**뿐이다 —
폰과 PC가 같은 Wi-Fi에 있어야 한다거나, PC의 LAN IP를 찾아 넣어야 하는 전제는 없다.

| 상황 | base URL |
|---|---|
| 로컬 개발 (같은 PC에서) | `http://localhost:8080` |
| 서버에 올린 뒤 (VPS 예정) | `http://<서버-주소>:8080` — 주소는 `(미정)` |

- Spring Boot는 기본으로 `0.0.0.0:8080`에 바인딩된다 (별도 설정 불필요).
- 응답의 `imageUrls`는 **상대 경로**이므로, 서버가 어디로 가든 **앱은 base URL 한 값만 바꾸면 된다.**
- 연결 확인: 브라우저에서 `<base URL>/swagger-ui.html`이 열리면 통신 OK.

### 평문 HTTP다 (HTTPS 아님)

지금 통신 구간은 `http://`다. Basic 인증은 매 요청에 `base64(아이디:비밀번호)`를 실어 보내고,
base64는 암호화가 아니라 되돌릴 수 있는 인코딩이다. 즉 **평문 구간에서는 비밀번호가 요청마다
노출되는 것과 같다.** 실제 개인정보를 넣지 말고 데모용 계정만 쓴다.

> 인증 *방식*을 JWT·세션으로 바꿔도 이 성질은 그대로다 — 평문이면 토큰도 같이 새어 나간다.
> 해결은 전송 구간을 HTTPS로 덮는 것이고, 그건 이 문서가 다루는 범위 밖이다.

### cleartext(HTTP) 허용

통신이 `http://`이므로 Expo 실기기 standalone 빌드에서 cleartext가 차단될 수 있다.
`app.json`에 `expo-build-properties`로 예외를 준다 (Expo Go 개발 중엔 대체로 통과).

```json
{
  "expo": {
    "plugins": [
      ["expo-build-properties", {
        "android": { "usesCleartextTraffic": true },
        "ios": { "infoPlist": { "NSAppTransportSecurity": { "NSAllowsArbitraryLoads": true } } }
      }]
    ]
  }
}
```

---

## 프론트엔드(Expo) 연동

패키지 설치:
```bash
npx expo install expo-image-picker expo-image-manipulator
```

### 서버 주소 설정
```ts
// api.ts — 서버 주소는 여기 한 곳에서만 관리한다
export const BASE_URL = "http://localhost:8080";           // ← 서버에 올린 뒤엔 그 주소로 교체
export const imageUri = (path: string) => `${BASE_URL}${path}`; // "/files/a.jpg" → 전체 URL
```

### 촬영 / 갤러리 다중 선택 + 리사이즈
```ts
import * as ImagePicker from "expo-image-picker";
import * as ImageManipulator from "expo-image-manipulator";

// 갤러리에서 여러 장 선택
export async function pickFromGallery() {
  const r = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ImagePicker.MediaTypeOptions.Images,
    allowsMultipleSelection: true,
    quality: 1,
  });
  return r.canceled ? [] : r.assets.map(a => a.uri);
}

// 카메라 촬영 (1장씩 — 여러 장이면 반복 호출해 누적)
export async function takePhoto() {
  await ImagePicker.requestCameraPermissionsAsync();
  const r = await ImagePicker.launchCameraAsync({ quality: 1 });
  return r.canceled ? null : r.assets[0].uri;
}

// 업로드 전 압축 (카메라 원본 수 MB → 필수)
async function compress(uri: string) {
  const out = await ImageManipulator.manipulateAsync(
    uri, [{ resize: { width: 1280 } }],
    { compress: 0.8, format: ImageManipulator.SaveFormat.JPEG }
  );
  return out.uri;
}
```

### 업로드 → URL 배열
```ts
import { BASE_URL } from "./api";

export async function uploadImages(uris: string[]): Promise<string[]> {
  const form = new FormData();
  for (const uri of uris) {
    const small = await compress(uri);
    // 파트 이름은 반드시 "files" (백엔드 @RequestPart("files"))
    form.append("files", { uri: small, name: "photo.jpg", type: "image/jpeg" } as any);
  }
  const res = await fetch(`${BASE_URL}/api/uploads`, {
    method: "POST",
    body: form,   // Content-Type 직접 지정 금지 — RN이 multipart boundary 자동 설정
  });
  const json = await res.json();
  return json.data.urls;   // ["/files/a.jpg", ...] → 등록 요청 imageUrls로 그대로 전달
}
```

### 표시
```tsx
import { imageUri } from "./api";
<Image source={{ uri: imageUri(space.imageUrls[0]) }} style={{ width: 120, height: 120 }} />
```

---

## 테스트 전략

### 1) 서버 단독 (curl / Swagger)
서버만으로 업로드→조회→등록을 먼저 검증한다 (앱 문제와 서버 문제 분리).
```bash
# 업로드
curl -u hong:pw1234 -F "files=@a.jpg" -F "files=@b.jpg" http://localhost:8080/api/uploads
#  → {"success":true,"data":{"urls":["/files/xxxx.jpg","/files/yyyy.jpg"]}}

# 조회 (정적 서빙 확인)
curl -o check.jpg http://localhost:8080/files/xxxx.jpg

# 등록에 URL 반영 확인 (활동 예시)
curl -X POST http://localhost:8080/api/activities -u hong:pw1234 \
  -H "Content-Type: application/json" \
  -d '{"field":"ART","title":"t","date":"2026-07-30",
       "startTime":"10:00","endTime":"12:00","capacity":4,"entryFee":0,
       "imageUrls":["/files/xxxx.jpg"]}'
#  → 응답 data.imageUrls 에 그대로 반환되면 OK
```
Swagger UI(`/swagger-ui.html`)의 **Upload** 태그에서 파일 선택 업로드도 가능.

### 2) 서버 연결 확인 (폰 브라우저)
앱 빌드 전에, 폰 브라우저에서 아래가 열리는지로 서버 통신부터 확인한다 (앱 문제와 통신 문제 분리).
- `<base URL>/swagger-ui.html`
- 업로드된 파일 URL `<base URL>/files/xxxx.jpg`

### 3) 앱 통합 (실기기/에뮬레이터)
- 촬영 1장 → 업로드 → 등록 → 목록/상세에서 이미지 표시 확인
- 갤러리 여러 장 → 순서대로 업로드/표시되는지 (`@OrderColumn` 순서 보존)
- 대용량 원본 그대로 올려 413(요청 초과) 발생 시: 앱 압축 적용 또는 `max-request-size` 조정
- 이미지가 안 뜨면 체크: base URL 오타 / 상대경로에 base URL 미부착 / cleartext(HTTP) 허용

### 흔한 실패와 원인
| 증상 | 원인 |
|---|---|
| 서버 접속 안 됨 | base URL 오타·포트 누락, 서버가 안 떠 있음 |
| 업로드는 되는데 이미지 안 보임 | 상대경로에 base URL 미부착 / cleartext HTTP 차단 |
| 413 Payload Too Large | 원본 대용량 — 앱에서 리사이즈, 한도 상향 |
| 401 `UNAUTHORIZED` | `Authorization` 헤더 누락, 또는 DB에 없는 계정 |

---

## 소유권 / 규칙

패키지별 담당·보안·Git 규칙은 [`CLAUDE.md`](./CLAUDE.md) 참고.
이미지 업로드 인프라(`common/storage`)와 다중 이미지 모델(`domain`)은 백엔드 리더(A)가 관리한다.
