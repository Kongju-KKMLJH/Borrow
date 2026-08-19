# 화면 ↔ API 매핑 분석

Figma 파일 `강김마이전홍` (`41vV5Ot8sn70att4zZsZMu`)의 `국문 v2 (클린)` 캔버스에서 추출한 16개 화면과 백엔드 REST API 매핑.

## 앱 개요

- **앱명**: 아트민 (천안 유휴공간 대여 서비스)
- **역할**: MEMBER(일반 회원), HOST(공간 제공자), ARTIST(예술가)
- **인증**: HTTP Basic (`Authorization: Basic base64(loginId:password)`)
- **하단 탭바 (사용자)**: 홈 / 참여하기 / 만들기 / 내 활동
- **하단 탭바 (제공자)**: 홈 / 요청 / 공간 관리

---

## 1. 스플래시 (96:989)

| 항목 | 값 |
|------|-----|
| 텍스트 | "아트민" |
| API 호출 | 없음 |

---

## 2. 홈 / 수정본 (396:449) — `/`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "아트민" + 역할 토글("공간 제공자" / "일반 사용자") | `GET /api/auth/me` (역할 확인) |
| 히어로 배너 | "함께할 취미, 가까운 공간에서" + "활동 참여하기" / "활동 만들기" 버튼 | — |
| 추천 활동 섹션 | "추천 활동" + "전체보기" + 필터("취미 모임" / "전문 클래스" / "그림" / "촬영") | `GET /api/activities?type=HOBBY&field=ART` |
| ActivityCard (3장) | "취미 모임" 배지, 분야("그림"), 제목, 진행자, 날짜·장소, "N / M명", 참가비 | ↑ |
| 천안 지역 활동 섹션 | "천안 지역 활동" + "전체보기" + "모집 중" 배지 카드 | `GET /api/activities?keyword=천안` (또는 전체 목록에서 지역 필터) |
| CTA | "원하는 활동이 없나요? 직접 취미 모임을 만들어보세요" + "활동 만들기" | — |
| 하단 탭 | 홈 / 참여하기 / 만들기 / 내 활동 | — |

**ActivityCard 데이터 → `ActivitySummaryResponse`**:
```
type=HOBBY/CLASS, field=ART/PHOTO, title, hostNickname, hostCertified,
date, startTime, endTime, capacity, currentHeadcount, entryFee, status,
imageUrls, alreadyJoined, mine
```

---

## 3. 활동 목록 (112:145) — `/activities`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "활동 참여하기" | — |
| 검색바 | "활동명을 검색해보세요" | `GET /api/activities?keyword={검색어}` |
| 결과 요약 | "총 24개 활동" + "추천순" 정렬 | ↑ |
| ActivityCard 리스트 | (홈과 동일 구조) + "모집 중" 배지 | `GET /api/activities?type=&field=&keyword=` |
| 하단 탭 | 홈 / 참여하기 / 만들기 / 내 활동 | — |

**필터 매핑**:
- "취미 모임" → `type=HOBBY`
- "전문 클래스" → `type=CLASS`
- "그림" → `field=ART`
- "촬영" → `field=PHOTO`

---

## 4. 활동 상세 (114:226) — `/activities/:id`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "활동 상세" | — |
| 배지 | "취미 모임" + "그림" + "모집 중" | `GET /api/activities/{id}` |
| 제목·설명 | "수채화로 그리는 주말 오후 드로잉" + 설명 | ↑ |
| 진행자 | "김서연" + "수채화 클래스 · 진행자" + "포트폴리오" | ↑ (`hostNickname`, `hostCertified`) |
| 활동 정보 | 활동 날짜 / 활동 시간 / 모집 마감 / 현재 인원 / 난이도 | ↑ (`date`, `startTime`~`endTime`, `capacity`/`currentHeadcount`) |
| 공간 정보 | "갤러리42" + 주소 + 시설(넓은 테이블, 자연광, 콘센트, 세면시설) + 이용조건 | ↑ (SpaceRequirement + 매칭된 Space 정보) |
| 참가비 | 참가비 / 공간 이용료 / 재료비 / 총 결제 금액 | ↑ (`entryFee`) |
| 준비물 및 안내 | 앞치마, 정리, 10분 전 도착 | — (설명 필드 또는 별도) |
| CTA | "참여 신청하기" (15,000원) | `POST /api/activities/{id}/participations` `{headcount: 1}` |

**→ `ActivityDetailResponse`**:
```
id, type, field, title, description, imageUrls, hostNickname, hostCertified,
date, startTime, endTime, capacity, currentHeadcount, entryFee, status,
requirement: {region, headcount, requiredFacilities, noisy, messy},
alreadyJoined, mine
```

---

## 5. 만들기 플로우 (117:234 ~ 124:309) — `/create`

6단계 워크플로우. 전체 진행 상태는 클라이언트에서 관리. 마지막 단계에서 백엔드 호출.

### 5-1. STEP 1 활동 유형 (117:234)

| UI | API |
|----|-----|
| "활동 만들기" / "STEP 1 / 6 · 활동 유형" | — |
| 분야 선택: "그림" / "촬영" | → `field=ART` / `field=PHOTO` (클라이언트 상태) |
| 세부 활동: 드로잉/수채화/캐릭터 그리기/기타 | — (UI only, description에 반영) |
| "다음" 버튼 | — |

### 5-2. STEP 2 활동 정보 (118:236)

| UI | API |
|----|-----|
| "STEP 2 / 6 · 활동 정보" | — |
| 활동 제목 입력 | → `title` |
| 활동 설명 입력 | → `description` |
| 활동 날짜 | → `date` (LocalDate) |
| 활동 시간 (시작) | → `startTime` (LocalTime) |
| 모집 인원 | → `capacity` |
| 참가비 | → `entryFee` |
| 난이도: 초보 환영 / 경험자 / 제한 없음 | — (UI only, description에 반영) |
| 준비물 | — (UI only) |
| "이전" / "다음" | — |

> **참고**: `type`·`hostCertified`·`hostNickname`은 서버가 로그인 역할에서 자동 채움. 요청 DTO에서 제외.

### 5-3. STEP 3 공간 조건 (119:228)

| UI | API |
|----|-----|
| "STEP 3 / 6 · 공간 조건" | — |
| 희망 지역 | → `requirement.region` |
| 활동 인원 (자동 입력됨) | → `requirement.headcount` |
| 필요한 시설 (다중 선택) | → `requirement.requiredFacilities: Set<FacilityType>` |
| 활동 특성: 소음 발생 가능 / 오염 가능 | → `requirement.noisy`, `requirement.messy` |
| 기타 요청사항 | → (description에 추가 또는 별도) |
| "이전" / "AI 분석하기" | — |

**FacilityType 매핑**:
- "넓은 테이블" → `TABLE`
- "의자" → (별도 enum 없음, conditions에 반영)
- "자연광" → `NATURAL_LIGHT`
- "촬영 조명" → `LIGHTING`
- "촬영 배경" → (별도 enum 없음, conditions에 반영)
- "물 사용" → `WATER`
- "세면시설" → (WATER에 포함 또는 conditions)
- "콘센트" → `OUTLET`

### 5-4. STEP 4 AI 분석 (121:236)

| UI | API |
|----|-----|
| "STEP 4 / 6 · AI 분석" / "AI 분석 완료" | — |
| 활동 요약 (유형, 날짜·시간, 인원, 난이도) | — (클라이언트 상태 표시) |
| AI 추출 공간 조건 | `POST /api/ai/analyze` `{description, region}` |
| → 필요한 공간 규모 / 필요 시설 / 허용 활동 / 추천 공간 유형 | → `RequirementResponse` |
| "조건이 맞지 않으면 위 항목을 직접 수정할 수 있어요." | (클라이언트에서 필드 수정 가능) |
| "이전" / "추천 공간 보기" | — |

**→ `RequirementResponse`**:
```
region, headcount, requiredFacilities: List<FacilityType>,
noisy, messy, field: ActivityField
```

### 5-5. STEP 5 공간 추천 (122:234)

| UI | API |
|----|-----|
| "STEP 5 / 6 · 공간 추천" / "조건에 맞는 유휴 공간 3곳을 찾았어요." | — |
| SpaceMatchCard 리스트 | `POST /api/ai/match` `{region, headcount, requiredFacilities, noisy, messy, field, date, startTime, endTime}` |
| 카드: 공간명, 지역·최대인원, 적합도(%), 추천 이유, 시설 충족, 이용조건, "이 공간 선택" | → `List<SpaceMatchResponse>` |
| "조건 다시 설정" | — (STEP 3로 이동) |

**→ `SpaceMatchResponse`**:
```
spaceId, name, region, capacity, hourlyFee, imageUrls,
facilities: Set<FacilityType>, allowedFields: Set<ActivityField>,
score (0~100), reason, aiScored
```

### 5-6. STEP 6 요청 확인 (123:309)

| UI | API |
|----|-----|
| "STEP 6 / 6 · 개최 요청" / "개최 요청을 확인해주세요" | — |
| 활동 정보 요약 (활동명, 유형·분야, 날짜·시간, 모집 인원, 참가비) | — (클라이언트 상태) |
| 선택한 공간 (공간명, 위치, 공간 이용료, 이용 조건) | — (STEP 5 선택값) |
| 전달될 요청사항 | — (description 또는 별도) |
| "이전" / "개최 요청 보내기" | 2단계: ① `POST /api/activities` (활동 개설) ② `POST /api/activities/{newId}/hosting-request` `{spaceId}` |

**활동 개설 → `ActivityCreateRequest`**:
```
field, title, description, imageUrls, date, startTime, endTime,
capacity, entryFee, requirement: {region, headcount, requiredFacilities, noisy, messy}
```

**개최 요청 → `HostingRequestCreateRequest`**:
```
spaceId
```

### 5-7. STEP 7 요청 완료 (124:309)

| UI | API |
|----|-----|
| "개최 요청을 보냈어요" / "공간 승인 대기 중" | — |
| "내 활동에서 확인" | → `/my-activities` 이동 |
| "홈으로 이동" | → `/` 이동 |

---

## 6. 내 활동 (125:309) — `/my-activities`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "내 활동" | — |
| 탭 | "참여한 활동" / "만든 활동" | — |
| 카드 (상태별) | "공간 승인 대기" / "모집 중" / "모집 마감" / "거절" | ↓ |
| 카드 내용 | 제목, 공간명·모집인원, 거절 사유 | ↓ |
| "대체 공간 추천받기" (거절 시) | → STEP 5 재진입 (excludeSpaceIds에 거절 공간 추가) | `POST /api/ai/match` `{..., excludeSpaceIds: [거절공간ID]}` |
| 하단 탭 | 홈 / 참여하기 / 만들기 / 내 활동 | — |

**"만든 활동" 탭**: `GET /api/me/activities` → `List<ActivitySummaryResponse>`
**"참여한 활동" 탭**: `GET /api/me/participations` → `List<MyParticipationResponse>`

**→ `MyParticipationResponse`**:
```
participationId, myHeadcount, activity: ActivitySummaryResponse
```

**상태 매핑** (ActivitySummaryResponse.status):
- `DRAFT` → "공간 승인 대기" (개최 요청 전송 후 PENDING)
- `PENDING` → "공간 승인 대기"
- `PUBLISHED` → "모집 중" (currentHeadcount < capacity) / "모집 마감" (currentHeadcount >= capacity)
- `REJECTED` → "거절"

---

## 7. 공간 제공자 홈 (127:309) — `/provider`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "아트민" + "제공자" / "일반 사용자" 토글 | `GET /api/auth/me` |
| 환영 | "사장님, 안녕하세요 👋" + 공간명 | ↑ |
| 현황 대시보드 | "이번 주 공간 현황" + 승인 대기(2) / 승인 활동(5) / 예정 활동(3) | `GET /api/host/home` |
| 내 공간 | 공간명, 지역·최대인원, "수정" 버튼 | `GET /api/spaces/mine` |
| 새로운 개최 요청 | "전체 보기" + RequestCard 리스트 (승인/거절 버튼) | `GET /api/host/requests?status=PENDING` |
| 예정된 활동 | 활동명, 날짜·시간, 인원 | `GET /api/host/schedules` |
| 하단 탭 | 홈 / 요청 / 공간 관리 | — |

**→ `HostHomeResponse`**:
```
pendingRequestCount, confirmedScheduleCount, spaceCount,
recentPendingRequests: List<HostingRequestResponse>,
upcomingSchedules: List<ScheduleResponse>
```

**RequestCard → `HostingRequestResponse`** (space.dto):
```
id, status: RequestStatus, rejectReason,
space: {id, name, region},
activity: {id, title, description, field, date, startTime, endTime, capacity, entryFee, hostNickname, requirement: {headcount, requiredFacilities, noisy, messy}}
```

**승인/거절 액션**:
- "승인" → `POST /api/host/requests/{id}/approve`
- "거절" → `POST /api/host/requests/{id}/reject` `{reason}`

---

## 8. 개최 요청 목록 (130:361) — `/provider/requests`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "개최 요청" | — |
| 상태 필터 탭 | "전체" / "승인 대기" / "승인" / "거절" | `GET /api/host/requests?status=` |
| RequestCard 리스트 | (제공자 홈과 동일 구조) + 승인/거절 버튼 | ↑ |
| 하단 탭 | 홈 / 요청 / 공간 관리 | — |

**상태 매핑**:
- "전체" → `status` 파라미터 생략
- "승인 대기" → `status=PENDING`
- "승인" → `status=APPROVED`
- "거절" → `status=REJECTED`

---

## 9. 요청 상세 (131:439) — `/provider/requests/:id`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "개최 요청 상세" | — |
| 배지 | "취미 모임" + "그림" + "승인 대기" | `GET /api/host/requests/{id}` |
| 제목·설명 | 활동명 + 설명 | ↑ |
| 진행자 | "김서연" + "진행자 · 활동 3회 개최" | ↑ (`activity.hostNickname`) |
| 일정 | 활동 날짜 / 활동 시간 / 공간 사용 예정 | ↑ (`activity.date`, `startTime`~`endTime`) |
| 참여 | 모집 인원 / 예상 참여자 / 난이도 | ↑ (`activity.capacity`) |
| 요청 시설 | 넓은 테이블·좌석 / 자연광 / 물 사용 / 콘센트 / 촬영 조명 / 세면시설 | ↑ (`activity.requirement.requiredFacilities`) |
| AI 매칭 분석 | "우리 공간과의 적합도 95%" + 사유 | (클라이언트에서 재계산 또는 별도 표시) |
| CTA | "개최 거절" / "개최 승인" | `POST /api/host/requests/{id}/approve` 또는 `/reject` |

---

## 10. 공간 관리 (133:439) — `/provider/space`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "공간 관리" | — |
| 기본 정보 | 공간 사진 추가 / 공간명 / 공간 주소 / 공간 소개 / 최대 수용 인원 | `GET /api/spaces/mine` → `PUT /api/spaces/{id}` |
| 제공 시설 | 보유 시설 다중 선택 (넓은 테이블, 의자, 자연광, 촬영 조명, 촬영 배경, 세면시설, 콘센트) | → `facilities: Set<FacilityType>` |
| 허용 활동 | 그림 활동 허용 / 촬영 활동 허용 / 물감 사용 허용 / 장비 사용 허용 / 소음 활동 제한 | → `allowedFields: Set<ActivityField>`, `noiseAllowed`, `messAllowed` |
| 제공 가능 시간 | 제공 요일(월~일) / 시작 시간 / 종료 시간 / "시간대 추가" | `POST/DELETE /api/spaces/{id}/slots` |
| 이용 조건 | 공간 이용료 / 최소 이용 시간 / 음료 주문 조건 / 정리 시간 / 기타 주의사항 | → `hourlyFee`, `conditions` |
| 저장 | (하단 저장 버튼) | `PUT /api/spaces/{id}` |

**→ `SpaceRequest`**:
```
name, region, address, imageUrls, capacity, hourlyFee, conditions,
facilities: Set<FacilityType>, allowedFields: Set<ActivityField>,
noiseAllowed, messAllowed
```

**→ `SpaceSlotRequest`**:
```
dayOfWeek: DayOfWeek, startTime: LocalTime, endTime: LocalTime
```

---

## 공통 응답 포맷

모든 API 응답은 `ApiResponse<T>` 래퍼:
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

에러 시:
```json
{
  "success": false,
  "data": null,
  "error": { "code": "UNAUTHORIZED", "message": "인증이 필요합니다." }
}
```

## 이미지 처리

1. **업로드**: `POST /api/uploads` (multipart, 파트명 `files`) → `{urls: ["/files/xxx.jpg"]}`
2. **등록**: 받은 URL 배열을 `imageUrls` 필드에 그대로 포함
3. **표시**: 상대 경로에 BASE_URL 결합 → `http://<PC-IP>:8080/files/xxx.jpg`

## 인증 헤더

모든 인증 필요 API에:
```
Authorization: Basic base64(loginId:password)
```

비로그인 허용: `POST /api/auth/signup`, `GET /api/activities`, `GET /api/activities/{id}`, `GET /api/spaces`, `GET /api/spaces/{id}`, `GET /api/spaces/{id}/slots`

---

## 11. 예술가 — 공간 매칭 완료 (396:925) — `/activities/:id/hosting`

> **백엔드 상태**: 호스트 승인 후 활동 자동 공개됨. 결제 단계 없음.
> Figma 디자인에는 승인 후 결제 단계가 있지만, 현재 백엔드는 `POST /api/host/requests/{id}/approve` 시 `activity.publish()` 자동 호출.

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "공간 승인 및 결제하기" | — |
| 진행자 정보 | "박민지" + "진행자 · 활동 3회 개최" | `GET /api/activities/{id}/hosting-request` (status=APPROVED 확인) |
| 활동 정보 | 활동 날짜 / 공간 사용 인원 / 공간 사용 예정(시간) | ↑ (`activity.date`, `capacity`, `startTime`~`endTime`) |
| 공간 정보 | "갤러리42" + 주소 + 공간 이용료 + 이용조건 | `GET /api/spaces/{spaceId}` |
| CTA | "결제하기" | ⚠️ **API 없음** — 결제 엔드포인트 미구현 |

**현재 백엔드 플로우**: 호스트 승인 → 활동 자동 PUBLISHED → 결제 없이 공개
**Figma 디자인 플로우**: 호스트 승인 → 예술가 결제 → 결제 완료 후 공개

---

## 12. 예술가 — 영수증 및 결제 금액 (396:812) — `/activities/:id/payment`

> **API 없음**: 결제/정산 시스템 미구현. 아래는 Figma 화면 분석만.

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "공간 승인 및 결제하기" | — |
| 공간 정보 | "갤러리42" + 주소 + 공간 이용료(30,000원) + 이용조건 | `GET /api/spaces/{spaceId}` (`hourlyFee`, `conditions`) |
| 승인 안내 | "공간 파트너가 개최 요청을 승인했습니다" | `GET /api/activities/{id}/hosting-request` (status=APPROVED) |
| 예상 운영 정산 | 예상 참가비 수익(150,000원) / 공간 이용료(-30,000원) / 아트민 매칭 이용료(-5,000원) / 예상 운영 수익(115,000원) | ⚠️ **API 없음** — 정산 계산 엔드포인트 미구현 |
| CTA | "결제 및 프로그램 공개" | ⚠️ **API 없음** — 결제 + 공개 엔드포인트 미구현 |

**정산 계산 로직** (클라이언트에서 임시 계산 가능):
```
예상 참가비 수익 = entryFee × capacity
공간 이용료 = space.hourlyFee (또는 activity에서 전달)
아트민 매칭 이용료 = 5,000원 (고정?)
예상 운영 수익 = 참가비 수익 - 공간 이용료 - 매칭 이용료
```

---

## 13. 예술가 — 결제 완료 (396:871) — `/activities/:id/payment/done`

> **API 없음**: 결제 완료 후 활동 공개 처리 미구현.

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 완료 | "결제 완료!" / "프로그램이 공개되었습니다!" | ⚠️ **API 없음** — 결제 완료 + 활동 PUBLISHED 전환 |
| CTA | "프로그램 보기" | → `/activities/:id` 이동 (`GET /api/activities/{id}`) |
| CTA | "더 둘러보기" | → `/` 이동 |

---

## 14. 구독제 — 소개 (421:550) — `/subscription`

> **API 없음**: 구독 시스템 전체 미구현.

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "아트민과 함께, 더 자유롭게" / "ARTMIN PRO" | — |
| 소개 | "프로그램 운영을 더 쉽고, 더 강력하게" | — |
| 혜택 목록 | 매칭 이용료 할인 / 우선 노출 / 정산 관리 강화 / PRO 전용 혜택 | — |
| 구독료 | "월 구독료 10,000원/ 월" | — |
| CTA | "구독하기" | ⚠️ **API 없음** — 구독 가입 엔드포인트 미구현 |

---

## 15. 구독제 — 결제 확인 (421:549) — `/subscription/confirm`

> **API 없음**: 구독 결제 미구현.

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 진행 표시 | 1(구독 소개) → 2(결제 확인) → 3(완료) | — |
| 선택 플랜 | "ARTMIN PRO" / "월 구독료 10,000원" | — |
| 결제 수단 | "카드 결제" | — |
| 결제 예정 | "다음 결제 예정일 2026년 10월 03일" / "매월 10일 자동 결제" | — |
| 총 결제 | "10,000원/ 월" | — |
| CTA | (다음) | ⚠️ **API 없음** — 구독 결제 엔드포인트 미구현 |

---

## 16. 구독제 — 완료 (421:552) — `/subscription/done`

> **API 없음**: 구독 완료 미구현.

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 완료 | "구독 완료!" / "이제 ARTMIN PRO 혜택으로 더 자유롭게 프로그램을 운영해보세요." | — |
| 결제 정보 | "다음 결제 예정일 2026년 10월 03일" / "매월 10일 자동 결제됩니다." | — |
| CTA | "프로그램 만들기" | → `/create` 이동 |
| CTA | "구독 관리하기" | ⚠️ **API 없음** — 구독 관리 엔드포인트 미구현 |

---

## API 구현 갭 요약

### 결제 시스템 (미구현)

Figma 디자인의 예술가 플로우: 호스트 승인 → 결제 → 활동 공개
현재 백엔드: 호스트 승인 → 활동 자동 공개 (결제 없음)

| 필요 API | 메서드 | 경로 | 설명 |
|----------|--------|------|------|
| 정산 미리보기 | GET | `/api/activities/{id}/settlement` | 예상 수익, 공간 이용료, 매칭 이용료 |
| 결제 처리 | POST | `/api/activities/{id}/payment` | 공간 이용료 + 매칭 이용료 결제 |
| 결제 완료 → 공개 | (위 결제 응답) | — | 결제 성공 시 activity PUBLISHED 전환 |

**백엔드 변경 필요**: `HostingRequest.approve()`에서 `activity.publish()` 자동 호출을 제거하고, 결제 완료 시 publish하도록 분리.

### 구독 시스템 (미구현)

| 필요 API | 메서드 | 경로 | 설명 |
|----------|--------|------|------|
| 구독 가입 | POST | `/api/subscriptions` | 플랜 선택 + 결제 수단 등록 |
| 구독 정보 조회 | GET | `/api/subscriptions/me` | 현재 구독 상태, 다음 결제일 |
| 구독 취소 | DELETE | `/api/subscriptions/me` | 구독 해지 |
| 구독 관리 | PATCH | `/api/subscriptions/me` | 결제 수단 변경 등 |

### 공간 관리 변형 (409:422)

`409:422`는 기존 10번 화면과 동일. 하단 "공간 정보 저장" 버튼이 프레임 하단에 고정되는 레이아웃 변형만 다름. API 매핑은 10번과 동일.

---

## 17~20. 관리자 콘솔 (기능명세 7) — `/(admin)`

피그마 화면이 아니라 **기능명세 7 · 유저플로우 "관리자 콘솔" 섹션**을 근거로 만든 화면들이다.
로그인 후 `app/index.tsx`의 역할 분기가 ADMIN을 여기로 보내고, `(admin)/_layout.tsx`가
ADMIN이 아닌 계정을 `(user)`로 되돌린다.

### 17. 관리자 홈 — `/(admin)`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 헤더 | "아트민" + "관리자 콘솔" + 로그아웃 | — |
| 현황 요약 | 회원 / 프로그램 / 공간 건수 | `GET /api/admin/users`, `/activities`, `/spaces` |
| 심사 대기 알림 | "예술가 인증 심사 N건" | `GET /api/admin/artist-verifications` (기본 PENDING) |
| 관리 메뉴 | 회원 관리 / 프로그램 관리 / 공간 관리 | — (라우팅) |

### 18. 회원 관리 (7.1) — `/(admin)/members`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 인증 심사 섹션 | 신청자 닉네임·아이디·포트폴리오 + "인증 승인" | `GET /api/admin/artist-verifications` → `POST .../{id}/approve` |
| 역할 필터 | "전체" / "시민" / "예술가" / "공간 파트너" | (클라이언트 필터 — 목록 API는 전체를 준다) |
| 회원 카드 | 닉네임 · 아이디 · 가입일 + 역할/인증 배지 | `GET /api/admin/users` |
| 수정·삭제 | 행마다 "수정" / "삭제"(확인 모달) | `PUT /api/admin/users/{userId}` · `DELETE .../{userId}` |

- **관리자(ADMIN) 계정 행에는 수정·삭제 버튼을 노출하지 않는다.** 서버도 403으로 막는다(콘솔 잠금 방지).
- 회원 삭제는 **연쇄 하드 삭제**다 — 그 회원의 인증 신청·참여 신청·개설 프로그램·등록 공간까지 함께 지워진다. 모달이 이를 경고한다.
- 인증 상태 `NONE`은 "신청한 적 없음" — 백엔드 enum에는 없고 응답 문자열로만 온다.

### 19. 프로그램 관리 (7.2) — `/(admin)/activities`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 상태 필터 | "전체" / "모집 중" / "승인 대기" | (클라이언트 필터) |
| 프로그램 카드 | 프로그램명 · 담당 예술가 · 공간 · 일정 · 정원 + 상태/유형 배지 | `GET /api/admin/activities` |
| 수정·삭제 | 행마다 "수정" / "삭제"(확인 모달) | `PUT /api/admin/activities/{id}` · `DELETE .../{id}` |

- 프로그램 삭제는 **연쇄 하드 삭제**다 — 참여 신청과 개최 요청이 함께 지워진다.
- 상태 라벨은 `src/lib/admin-format.ts`의 `activityStatusLabel`이 매핑한다. 백엔드에 이미 있는
  `MATCHED`가 프론트 `ActivityStatus` 타입에는 아직 없어, 매핑에 없는 값은 원문을 그대로 보여준다.

### 20. 공간 관리 (7.3) — `/(admin)/spaces`

| UI 영역 | 텍스트 | API |
|---------|--------|-----|
| 공간 카드 | 공간명 · 등록자 · 등록일 · **주소 전문** · 수용 인원 · 시간당 이용료 | `GET /api/admin/spaces` |
| 수정·삭제 | 행마다 "수정" / "삭제"(확인 모달) | `PUT /api/admin/spaces/{id}` · `DELETE .../{id}` |

- **주소 전문은 관리자 응답에만 있다.** 공개 공간 응답(`GET /api/spaces`)은 여전히 동 단위(`region`)까지다.
- 공간 삭제는 **연쇄 하드 삭제**다 — 이용 가능 시간과 개최 요청이 함께 지워지고, 이 공간에서 승인됐던 프로그램은 `REJECTED`로 되돌아간다(재요청 가능).

### 21~23. 관리자 데이터 폼 (7.1.2 · 7.2.2 · 7.3.2) — `/admin-form/*`

목록 화면 3개의 생성 버튼("회원 생성" · "프로그램 생성" · "공간 생성")과 각 행의 "수정" 버튼이 여기로 온다.
`(admin)` 탭 밖의 스택 화면이라 `admin-form/_layout.tsx`가 ADMIN 가드를 한 번 더 건다.

| 화면 | 경로 | API |
|---|---|---|
| 회원 폼 | `/admin-form/user`(+`?userId=`) | `POST /api/admin/users` · `PUT .../{userId}` |
| 프로그램 폼 | `/admin-form/activity`(+`?activityId=`) | `POST /api/admin/activities` · `PUT .../{activityId}` |
| 공간 폼 | `/admin-form/space`(+`?spaceId=`) | `POST /api/admin/spaces` · `PUT .../{spaceId}` |

생성·수정 대상은 **실제 객체**다(별도의 임시 데이터 개념은 없다). 삭제는 목록 화면에서 `ConfirmModal` → `DELETE /api/admin/{...}/{id}`.

**폼이 받지 않는 것과 그 이유**

- 프로그램의 **유형(HOBBY/CLASS)·인증 배지**: 담당 예술가 계정의 역할을 보고 서버가 정한다. 클라이언트가 배지를 위조하지 못하게 하는 기존 규칙 그대로다.
- 회원의 **아이디(수정 시)**: `loginId`가 활동·참여·공간의 소유자 키라 바꾸면 그 회원의 데이터가 주인을 잃는다.
- 예술가가 아닌 회원의 **인증 상태**: 서버가 거절하므로 폼도 역할이 ARTIST일 때만 노출한다.

**주의**

- 프로그램 상태를 `MATCHED`·`PUBLISHED`로 두려면 **공간 id가 필수**다(개최지는 승인된 개최 요청으로만 표현된다). 폼이 저장 버튼을 막고 안내한다.
- 공간의 **이용 가능 요일·시간**은 선택한 요일마다 같은 시간대로 깔린다. 이게 없으면 AI 추천 후보에 걸리지 않는다.
- 수정 시 대상은 **목록 API에서 찾는다** — 관리자용 단건 조회 API는 만들지 않았다.
