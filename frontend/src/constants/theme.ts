/**
 * Borrow 디자인 시스템
 * 팝업/클래스 공간 매칭 앱. 웜 크림 배경 + 테라코타/틸/골드 팔레트.
 * 색상은 Figma "국문" 화면 세트에서 추출.
 */

import '@/global.css';

import { Platform } from 'react-native';

/**
 * Pretendard 폰트 패밀리. `assets/fonts`에서 로드되며 이름은 useFonts 키와 일치해야 한다.
 */
export const FontFamily = {
  regular: 'Pretendard-Regular',
  medium: 'Pretendard-Medium',
  semibold: 'Pretendard-SemiBold',
  bold: 'Pretendard-Bold',
} as const;

/**
 * 브랜드 팔레트 (light 기준). 대부분의 화면이 웜 라이트 모드로 디자인됨.
 */
const palette = {
  // Primary — 코랄 레드 (#F4475C): CTA·활성 상태·강조. Figma v2 팔레트 Main.
  terracotta: '#F4475C',
  terracottaPressed: '#DA3A4E',
  terracottaSoft: '#FDE7EA',
  // Secondary — 틸 그린: 성공·예약 상태 (절제해서 사용)
  teal: '#1FA98F',
  tealPressed: '#178471',
  tealSoft: '#E4F5F0',
  // Accent — 골드: 하이라이트 (배너 채움 대신 아이콘·수치에 소량)
  gold: '#F2A93B',
  goldPressed: '#E0982B',
  goldSoft: '#FDF1DD',
  // Toss식 쿨 뉴트럴 (Gray scale)
  cream: '#FFFFFF', // 페이지 배경 = 화이트
  white: '#FFFFFF',
  sand: '#F2F4F6', // muted surface (gray-100)
  sandDeep: '#E5E8EB', // border/deep (gray-200)
  ink: '#191F28', // 기본 텍스트 (gray-900)
  inkSoft: '#4E5968', // 보조 텍스트 (gray-700)
  inkMuted: '#8B95A1', // 흐린 텍스트 (gray-500)
  border: '#EFF1F4', // hairline
  // States
  danger: '#F04452',
  dangerSoft: '#FDECEE',
  star: '#F2A93B',
} as const;

export const Colors = {
  light: {
    // 배경 계층
    background: palette.cream,
    surface: palette.white,
    surfaceMuted: palette.sand,
    surfaceDeep: palette.sandDeep,
    // 텍스트
    text: palette.ink,
    textSecondary: palette.inkSoft,
    textMuted: palette.inkMuted,
    textInverse: palette.white,
    // 브랜드
    primary: palette.terracotta,
    primaryPressed: palette.terracottaPressed,
    primarySoft: palette.terracottaSoft,
    secondary: palette.teal,
    secondaryPressed: palette.tealPressed,
    secondarySoft: palette.tealSoft,
    accent: palette.gold,
    accentPressed: palette.goldPressed,
    accentSoft: palette.goldSoft,
    // 기타
    border: palette.border,
    danger: palette.danger,
    dangerSoft: palette.dangerSoft,
    success: palette.teal,
    star: palette.star,
    // 하위호환 (템플릿 컴포넌트)
    backgroundElement: palette.sand,
    backgroundSelected: palette.sandDeep,
  },
  dark: {
    background: '#1A1512',
    surface: '#241E1A',
    surfaceMuted: '#2E2721',
    surfaceDeep: '#3A322B',
    text: '#F5EFE9',
    textSecondary: '#C3B8AE',
    textMuted: '#8B8078',
    textInverse: '#201A17',
    primary: palette.terracotta,
    primaryPressed: palette.terracottaPressed,
    primarySoft: '#3A2620',
    secondary: palette.teal,
    secondaryPressed: palette.tealPressed,
    secondarySoft: '#1E332F',
    accent: palette.gold,
    accentPressed: palette.goldPressed,
    accentSoft: '#3A3220',
    border: '#3A322B',
    danger: palette.danger,
    dangerSoft: '#3A2020',
    success: palette.teal,
    star: palette.star,
    backgroundElement: '#2E2721',
    backgroundSelected: '#3A322B',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

/**
 * 타이포 프리셋. AppText 컴포넌트에서 variant로 사용.
 */
export const Typography = {
  display: { fontFamily: FontFamily.bold, fontSize: 28, lineHeight: 36, letterSpacing: -0.5 },
  h1: { fontFamily: FontFamily.bold, fontSize: 24, lineHeight: 32, letterSpacing: -0.4 },
  h2: { fontFamily: FontFamily.bold, fontSize: 20, lineHeight: 28, letterSpacing: -0.3 },
  h3: { fontFamily: FontFamily.semibold, fontSize: 18, lineHeight: 26, letterSpacing: -0.2 },
  title: { fontFamily: FontFamily.semibold, fontSize: 16, lineHeight: 24, letterSpacing: -0.1 },
  body: { fontFamily: FontFamily.regular, fontSize: 15, lineHeight: 23 },
  bodyStrong: { fontFamily: FontFamily.semibold, fontSize: 15, lineHeight: 23 },
  label: { fontFamily: FontFamily.medium, fontSize: 14, lineHeight: 20 },
  caption: { fontFamily: FontFamily.medium, fontSize: 13, lineHeight: 18 },
  small: { fontFamily: FontFamily.medium, fontSize: 12, lineHeight: 16 },
  tiny: { fontFamily: FontFamily.semibold, fontSize: 11, lineHeight: 14, letterSpacing: 0.2 },
} as const;

export type TypographyVariant = keyof typeof Typography;

/** 4pt 기반 spacing 스케일 */
export const Spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  xxxl: 32,
  huge: 48,
  // 하위호환 (템플릿)
  half: 2,
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

export const Radius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 28,
  full: 999,
} as const;

/** 토스식 초경량 섀도우 — 거의 플랫, 쿨그레이. 구분은 주로 여백·연회색 블록으로. */
export const Shadow = {
  none: {},
  sm: {
    shadowColor: '#2E3A4E',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.04,
    shadowRadius: 3,
    elevation: 1,
  },
  md: {
    shadowColor: '#2E3A4E',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.06,
    shadowRadius: 12,
    elevation: 3,
  },
  lg: {
    shadowColor: '#2E3A4E',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.08,
    shadowRadius: 20,
    elevation: 6,
  },
} as const;

/** 템플릿 컴포넌트 하위호환용 폰트 맵 */
export const Fonts = Platform.select({
  ios: { sans: 'Pretendard-Regular', serif: 'ui-serif', rounded: 'ui-rounded', mono: 'ui-monospace' },
  default: { sans: 'Pretendard-Regular', serif: 'serif', rounded: 'normal', mono: 'monospace' },
  web: {
    sans: 'Pretendard, var(--font-display)',
    serif: 'var(--font-serif)',
    rounded: 'var(--font-rounded)',
    mono: 'var(--font-mono)',
  },
})!;

export const BottomTabInset = Platform.select({ ios: 50, android: 80 }) ?? 0;
export const MaxContentWidth = 480;
