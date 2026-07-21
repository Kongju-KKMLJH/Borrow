import { Text, type TextProps } from 'react-native';

import { Typography, type TypographyVariant, type ThemeColor } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export type AppTextProps = TextProps & {
  variant?: TypographyVariant;
  color?: ThemeColor;
  /** 직접 색상값을 넘길 때 (color 토큰보다 우선) */
  tint?: string;
  center?: boolean;
};

/**
 * 앱 전역 텍스트. Pretendard 타이포 프리셋 + 테마 색상을 적용한다.
 */
export function AppText({
  variant = 'body',
  color = 'text',
  tint,
  center,
  style,
  ...rest
}: AppTextProps) {
  const theme = useTheme();
  return (
    <Text
      style={[
        Typography[variant],
        { color: tint ?? theme[color] },
        center && { textAlign: 'center' },
        style,
      ]}
      {...rest}
    />
  );
}
