# Borrow Backend

천안 유휴공간 대여 서비스의 Spring Boot 백엔드 (해커톤 MVP).
로그인 없이 프론트가 생성한 UUID를 `X-Guest-Id` 헤더로 사용자 식별.

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

## 이미지 업로드 (모바일 앱 MVP)

앱(Expo)에서 카메라 촬영·갤러리 선택 이미지를 **여러 장** 올리고 URL로 조회하는 구조.
현재는 **배포 없이 LAN의 PC 한 대를 서버로** 쓰는 것을 전제로, 파일을 서버 로컬 디스크에 저장한다.

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
- 앱은 앞에 서버 base URL(LAN IP)을 붙여 표시한다: `http://<PC-IP>:8080/files/a.jpg`

---

## LAN 실행 전략 (같은 Wi-Fi의 PC = 서버)

폰과 서버 PC를 같은 Wi-Fi에 두고, 앱이 PC의 LAN IP로 접속한다.

### 서버 PC 준비

1. 서버 실행: `docker compose up -d && ./gradlew bootRun`
   - Spring Boot는 기본으로 `0.0.0.0:8080`에 바인딩되어 LAN에 노출된다 (별도 설정 불필요).
2. PC의 LAN IP 확인:
   - macOS: `ipconfig getifaddr en0` (유선은 `en1` 등)
   - Windows: `ipconfig` → IPv4 주소
3. **방화벽**에서 8080 인바운드 허용 (막혀 있으면 폰에서 연결 실패).
4. 확인: 폰 브라우저에서 `http://<PC-IP>:8080/swagger-ui.html` 가 열리면 성공.

### 폰(앱) 준비

- **같은 Wi-Fi**에 접속 (게스트/AP 격리 네트워크는 기기 간 통신이 막힐 수 있음 — 주의).
- Android 에뮬레이터는 호스트 PC를 `10.0.2.2`로 접근 (`localhost` 아님).
- iOS 시뮬레이터는 `localhost`로 접근 가능.

### HTTP(비-HTTPS) 허용

로컬 서버는 `http://`이므로 실기기 standalone 빌드에서 cleartext가 차단될 수 있다.
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
// api.ts — LAN의 PC 서버 주소
export const BASE_URL = "http://192.168.0.10:8080";        // ← PC의 LAN IP로 교체
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
curl -F "files=@a.jpg" -F "files=@b.jpg" http://localhost:8080/api/uploads
#  → {"success":true,"data":{"urls":["/files/xxxx.jpg","/files/yyyy.jpg"]}}

# 조회 (정적 서빙 확인)
curl -o check.jpg http://localhost:8080/files/xxxx.jpg

# 등록에 URL 반영 확인 (활동 예시)
curl -X POST http://localhost:8080/api/activities \
  -H "Content-Type: application/json" -H "X-Guest-Id: test-guest" \
  -d '{"hostNickname":"t","field":"ART","title":"t","date":"2026-07-30",
       "startTime":"10:00","endTime":"12:00","capacity":4,"entryFee":0,
       "imageUrls":["/files/xxxx.jpg"]}'
#  → 응답 data.imageUrls 에 그대로 반환되면 OK
```
Swagger UI(`/swagger-ui.html`)의 **Upload** 태그에서 파일 선택 업로드도 가능.

### 2) LAN 연결 확인 (폰 브라우저)
앱 빌드 전에, 폰 브라우저에서 아래가 열리는지로 네트워크/방화벽부터 확인한다.
- `http://<PC-IP>:8080/swagger-ui.html`
- 업로드된 파일 URL `http://<PC-IP>:8080/files/xxxx.jpg`

### 3) 앱 통합 (실기기/에뮬레이터)
- 촬영 1장 → 업로드 → 등록 → 목록/상세에서 이미지 표시 확인
- 갤러리 여러 장 → 순서대로 업로드/표시되는지 (`@OrderColumn` 순서 보존)
- 대용량 원본 그대로 올려 413(요청 초과) 발생 시: 앱 압축 적용 또는 `max-request-size` 조정
- 이미지가 안 뜨면 체크: base URL 오타 / 같은 Wi-Fi 여부 / 방화벽 / cleartext(HTTP) 허용

### 흔한 실패와 원인
| 증상 | 원인 |
|---|---|
| 폰에서 서버 접속 안 됨 | 다른 Wi-Fi, AP 격리, PC 방화벽 8080 차단 |
| 업로드는 되는데 이미지 안 보임 | 상대경로에 base URL 미부착 / cleartext HTTP 차단 |
| 413 Payload Too Large | 원본 대용량 — 앱에서 리사이즈, 한도 상향 |
| `localhost`로 안 됨 | 에뮬레이터는 `10.0.2.2`, 실기기는 PC LAN IP 사용 |

---

## 소유권 / 규칙

패키지별 담당·보안·Git 규칙은 [`CLAUDE.md`](./CLAUDE.md) 참고.
이미지 업로드 인프라(`common/storage`)와 다중 이미지 모델(`domain`)은 백엔드 리더(A)가 관리한다.
