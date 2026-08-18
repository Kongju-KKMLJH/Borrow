import { Ionicons } from '@expo/vector-icons';
import { Pressable, View, type StyleProp, type ViewStyle } from 'react-native';

import { Radius, Spacing, type ThemeColor } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

import { AppText } from './app-text';

type Tone = 'primary' | 'secondary' | 'accent' | 'neutral' | 'danger' | 'solid';

/** 작은 상태 라벨. tone에 따라 soft 배경 + 진한 텍스트. */
export function Badge({
  label,
  tone = 'primary',
  icon,
  style,
}: {
  label: string;
  tone?: Tone;
  icon?: keyof typeof Ionicons.glyphMap;
  style?: StyleProp<ViewStyle>;
}) {
  const theme = useTheme();
  const map: Record<Tone, { bg: string; fg: string }> = {
    primary: { bg: theme.primarySoft, fg: theme.primary },
    secondary: { bg: theme.secondarySoft, fg: theme.secondary },
    accent: { bg: theme.accentSoft, fg: theme.accentPressed },
    neutral: { bg: theme.surfaceMuted, fg: theme.textSecondary },
    danger: { bg: theme.dangerSoft, fg: theme.danger },
    solid: { bg: theme.text, fg: theme.textInverse },
  };
  const c = map[tone];
  return (
    <View
      style={[
        {
          flexDirection: 'row',
          alignItems: 'center',
          gap: 3,
          alignSelf: 'flex-start',
          backgroundColor: c.bg,
          paddingHorizontal: Spacing.sm,
          paddingVertical: 4,
          borderRadius: Radius.full,
        },
        style,
      ]}>
      {icon && <Ionicons name={icon} size={12} color={c.fg} />}
      <AppText variant="tiny" tint={c.fg}>
        {label}
      </AppText>
    </View>
  );
}

/** 선택 가능한 필터/카테고리 칩. */
export function Chip({
  label,
  active,
  onPress,
  icon,
}: {
  label: string;
  active?: boolean;
  onPress?: () => void;
  icon?: keyof typeof Ionicons.glyphMap;
}) {
  const theme = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        gap: Spacing.xs,
        paddingHorizontal: Spacing.md,
        paddingVertical: Spacing.sm,
        borderRadius: Radius.full,
        backgroundColor: active ? theme.text : theme.surfaceMuted,
        borderWidth: 1,
        borderColor: active ? theme.text : theme.border,
      }}>
      {icon && (
        <Ionicons name={icon} size={14} color={active ? theme.textInverse : theme.textSecondary} />
      )}
      <AppText variant="caption" tint={active ? theme.textInverse : theme.textSecondary}>
        {label}
      </AppText>
    </Pressable>
  );
}

/** 색상 토큰 배경 위 텍스트를 쓰는 solid pill (예: 지금 예약 가능) */
export function Tag({ label, color = 'secondary' }: { label: string; color?: ThemeColor }) {
  const theme = useTheme();
  return (
    <View
      style={{
        alignSelf: 'flex-start',
        backgroundColor: theme[color],
        paddingHorizontal: Spacing.sm,
        paddingVertical: 4,
        borderRadius: Radius.sm,
      }}>
      <AppText variant="tiny" tint={theme.textInverse}>
        {label}
      </AppText>
    </View>
  );
}
