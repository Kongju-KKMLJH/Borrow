/**
 * Learn more about light and dark modes:
 * https://docs.expo.dev/guides/color-schemes/
 */

import { Colors } from '@/constants/theme';

/**
 * 현재 디자인은 라이트 전용이라 시스템 다크와 무관하게 라이트 팔레트를 반환한다.
 * 추후 다크 지원 시 useColorScheme으로 분기.
 */
export function useTheme() {
  return Colors.light;
}
