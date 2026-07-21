/**
 * 목업 데이터. API 연동 전 UI 개발용. api/ 서비스 레이어에서만 참조한다.
 */
import type { Activity, HostingRequest, Space, SpaceMatch, User } from './types';

const hosts = {
  seoyeon: { id: 'h1', name: '김서연', verifiedArtist: true, bio: '수채화 클래스 · 진행자' },
  junho: { id: 'h2', name: '이준호', verifiedArtist: false, bio: '사진 산책 모임장' },
  minji: { id: 'h3', name: '박민지', verifiedArtist: true, bio: '드로잉 아티스트' },
};

export const spaces: Space[] = [
  { id: 's1', name: '브루 랩 카페', address: '천안 서북구 두정동 100', district: '두정동', capacity: 12, facilities: ['넓은 테이블', '자연광', '콘센트', '세면시설'], pricePerHour: 15000, notes: '음료 1잔 주문 필수 · 정리 후 퇴실' },
  { id: 's2', name: '갤러리42', address: '천안 동남구 중앙로 108', district: '중앙로', capacity: 15, facilities: ['전시 벽면', '자연광', '전시 조명'], pricePerHour: 18000, notes: '물감 사용 시 바닥 보호 매트 사용' },
  { id: 's3', name: '인디고 비스트로', address: '천안 동남구 대학로 87', district: '대학로', capacity: 20, facilities: ['넓은 홀', '주방', '콘센트'], pricePerHour: 25000 },
];

export const activities: Activity[] = [
  {
    id: 'a1', type: 'hobby', field: 'drawing', title: '수채화로 그리는 주말 오후 드로잉',
    intro: '번지는 물의 느낌을 살려 나만의 감성 수채화를 완성하는 원데이 모임입니다. 초보자도 편하게 참여할 수 있어요.',
    description: '기본 채색법부터 간단한 풍경 완성까지 함께 그려봐요.',
    host: hosts.seoyeon, date: '10월 24일 (토)', time: '오후 2:00 ~ 4:00', deadline: '10월 22일 (목)',
    capacity: 8, joined: 3, fee: 15000, difficulty: 'beginner', status: 'recruiting', region: '천안',
    space: spaces[0], preparation: ['앞치마 또는 편한 복장', '작업 후 개인 소지품 정리', '시작 10분 전 도착'], joinStatus: 'available',
  },
  {
    id: 'a2', type: 'hobby', field: 'photo', title: '필름 감성 동네 사진 산책',
    intro: '원도심 골목을 걸으며 필름 감성 사진을 담는 모임이에요.',
    host: hosts.junho, date: '10월 26일 (월)', time: '오전 10:00 ~ 12:00', deadline: '10월 24일 (토)',
    capacity: 10, joined: 5, fee: 12000, difficulty: 'any', status: 'recruiting', region: '천안',
    space: spaces[2], joinStatus: 'available',
  },
  {
    id: 'a3', type: 'class', field: 'drawing', title: '초보 환영 인물 드로잉 클래스',
    intro: '인물의 비율과 표현을 배우는 전문 클래스입니다.',
    host: hosts.minji, date: '10월 28일 (수)', time: '오후 7:00 ~ 9:00', deadline: '10월 26일 (월)',
    capacity: 6, joined: 2, fee: 20000, difficulty: 'beginner', status: 'recruiting', region: '천안',
    space: spaces[0], joinStatus: 'available',
  },
];

/** AI 공간 추천 결과 (만들기 Step5) */
export const spaceMatches: SpaceMatch[] = [
  { space: spaces[0], score: 95, reason: '자연광이 풍부하고 넓은 테이블이 있어 드로잉 활동에 적합해요.', matched: ['테이블·자연광·물 사용·콘센트 모두 충족'], cautions: ['음료 1잔 주문 필수 · 정리 후 퇴실'] },
  { space: spaces[1], score: 88, reason: '층고가 높고 채광이 좋아 여유로운 작업이 가능해요.', matched: ['전시 벽면·자연광 충족'], cautions: ['물감 사용 시 매트 필요'] },
  { space: spaces[2], score: 82, reason: '넓은 홀로 인원 여유가 있으나 자연광이 다소 부족해요.', matched: ['수용 인원 충족'], cautions: ['자연광 부족'] },
];

export const requests: HostingRequest[] = [
  {
    id: 'r1', activity: activities[0], requester: hosts.seoyeon,
    requestedTime: '10월 24일 (토) 오후 2:00 ~ 4:00', spaceUseTime: '오후 1:30 ~ 4:30', headcount: 8,
    matchScore: 95, matched: ['필요 시설(테이블·자연광·물·콘센트)을 모두 충족해요'],
    cautions: ['물감 사용으로 오염 가능성이 있어요 · 정리 시간 확인 필요'], status: 'pending',
  },
  {
    id: 'r2', activity: activities[1], requester: hosts.junho,
    requestedTime: '10월 26일 (월) 오전 10:00 ~ 12:00', spaceUseTime: '오전 9:30 ~ 12:30', headcount: 10,
    matchScore: 89, matched: ['넓은 홀 · 콘센트 충족'], cautions: ['외부 촬영 이동 동선 확인'], status: 'pending',
  },
];

/** 로그인 사용자 (해커톤: 고정) */
export const currentUser: User = { id: 'u1', name: '김서연', businessName: '브루 랩 카페' };

/** 제공자 홈 요약 */
export const providerSummary = { pending: 2, approved: 5, upcoming: 3, space: spaces[0] };

/** 내 활동 */
export const myJoined: Activity[] = [activities[0], activities[1]];
export const myCreated: Activity[] = [
  { ...activities[0], status: 'pending', joined: 0 },
  { ...activities[1], id: 'a2c', status: 'recruiting', joined: 5 },
  { ...activities[2], id: 'a3c', title: '캘리그래피 엽서 만들기', status: 'closed', joined: 8, capacity: 8 },
  { ...activities[2], id: 'a4c', title: '주말 아침 원데이 유화', status: 'rejected', joined: 0 },
];
