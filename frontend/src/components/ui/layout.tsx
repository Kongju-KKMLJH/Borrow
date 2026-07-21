import { Ionicons } from '@expo/vector-icons';
import { Pressable, View, type StyleProp, type ViewStyle } from 'react-native';

import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

import { AppText } from './app-text';

/** 원형 아이콘 버튼 (헤더 뒤로가기·좋아요·공유 등) */
export function IconButton({
  icon,
  onPress,
  tone = 'surface',
  size = 40,
  color,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  onPress?: () => void;
  tone?: 'surface' | 'muted' | 'ghost';
  size?: number;
  color?: string;
}) {
  const theme = useTheme();
  const bg = tone === 'ghost' ? 'transparent' : tone === 'muted' ? theme.surfaceMuted : theme.surface;
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => ({
        width: size,
        height: size,
        borderRadius: Radius.full,
        backgroundColor: bg,
        alignItems: 'center',
        justifyContent: 'center',
        opacity: pressed ? 0.6 : 1,
      })}>
      <Ionicons name={icon} size={size * 0.5} color={color ?? theme.text} />
    </Pressable>
  );
}

/** 섹션 제목 + 우측 "모두 보기" 액션 */
export function SectionHeader({
  title,
  actionLabel,
  onAction,
  style,
}: {
  title: string;
  actionLabel?: string;
  onAction?: () => void;
  style?: StyleProp<ViewStyle>;
}) {
  const theme = useTheme();
  return (
    <View
      style={[
        {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: Spacing.md,
        },
        style,
      ]}>
      <AppText variant="h3">{title}</AppText>
      {actionLabel && (
        <Pressable
          onPress={onAction}
          hitSlop={8}
          style={{ flexDirection: 'row', alignItems: 'center', gap: 2 }}>
          <AppText variant="caption" color="textSecondary">
            {actionLabel}
          </AppText>
          <Ionicons name="chevron-forward" size={14} color={theme.textSecondary} />
        </Pressable>
      )}
    </View>
  );
}

/** 상단 브랜드 바 — 좌측 Borrow 로고 + 우측 액션(역할 전환 등) */
export function BrandBar({
  right,
  onLogoPress,
}: {
  right?: React.ReactNode;
  onLogoPress?: () => void;
}) {
  const theme = useTheme();
  return (
    <View
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: Spacing.xl,
        paddingVertical: Spacing.md,
      }}>
      <Pressable onPress={onLogoPress} style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
        <Ionicons name="cube" size={22} color={theme.primary} />
        <AppText variant="h2" tint={theme.primary}>
          Borrow
        </AppText>
      </Pressable>
      {right}
    </View>
  );
}

/** "역할 전환" 알약 버튼 */
export function RoleSwitch({ label = '역할 전환', onPress }: { label?: string; onPress?: () => void }) {
  const theme = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => ({
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
        borderWidth: 1,
        borderColor: theme.border,
        backgroundColor: theme.surface,
        paddingHorizontal: Spacing.md,
        paddingVertical: 6,
        borderRadius: Radius.full,
        opacity: pressed ? 0.7 : 1,
      })}>
      <Ionicons name="swap-horizontal" size={14} color={theme.textSecondary} />
      <AppText variant="small" color="textSecondary">
        {label}
      </AppText>
    </Pressable>
  );
}

export function Divider({ style }: { style?: StyleProp<ViewStyle> }) {
  const theme = useTheme();
  return <View style={[{ height: 1, backgroundColor: theme.border }, style]} />;
}
