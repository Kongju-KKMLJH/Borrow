import { Ionicons } from '@expo/vector-icons';
import { View, type StyleProp, type ViewStyle } from 'react-native';

import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

import { AppText } from './app-text';

/** 별점 + 리뷰 수 */
export function Rating({
  value,
  count,
  size = 13,
}: {
  value: number;
  count?: number;
  size?: number;
}) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 3 }}>
      <Ionicons name="star" size={size} color={theme.star} />
      <AppText variant="caption" color="text">
        {value.toFixed(1)}
      </AppText>
      {count != null && (
        <AppText variant="caption" color="textMuted">
          ({count})
        </AppText>
      )}
    </View>
  );
}

/** ₩ 가격 + 단위 (예: ₩12,000 / 시간) */
export function PriceTag({
  amount,
  unit = '시간',
  size = 'md',
  color,
}: {
  amount: number;
  unit?: string;
  size?: 'sm' | 'md' | 'lg';
  color?: string;
}) {
  const theme = useTheme();
  const priceVariant = size === 'lg' ? 'h2' : size === 'sm' ? 'title' : 'h3';
  return (
    <View style={{ flexDirection: 'row', alignItems: 'baseline', gap: 2 }}>
      <AppText variant={priceVariant} tint={color ?? theme.text}>
        ₩{amount.toLocaleString()}
      </AppText>
      {unit ? (
        <AppText variant="small" color="textMuted">
          /{unit}
        </AppText>
      ) : null}
    </View>
  );
}

/** AI 매칭 점수 pill — 흰 배경 + 초록 % */
export function MatchScore({
  score,
  style,
}: {
  score: number;
  style?: StyleProp<ViewStyle>;
}) {
  const theme = useTheme();
  return (
    <View
      style={[
        {
          flexDirection: 'row',
          alignItems: 'center',
          gap: 3,
          backgroundColor: theme.surface,
          paddingHorizontal: Spacing.sm,
          paddingVertical: 4,
          borderRadius: Radius.full,
          ...{
            shadowColor: '#000',
            shadowOpacity: 0.12,
            shadowRadius: 6,
            shadowOffset: { width: 0, height: 2 },
            elevation: 3,
          },
        },
        style,
      ]}>
      <Ionicons name="flash" size={12} color={theme.secondary} />
      <AppText variant="tiny" tint={theme.secondary}>
        {score}% 매칭
      </AppText>
    </View>
  );
}

/** 아이콘 + 수치 + 라벨 세로 스탯 (공간상세 면적/수용 등) */
export function Stat({
  icon,
  value,
  label,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  value: string;
  label: string;
}) {
  const theme = useTheme();
  return (
    <View style={{ gap: Spacing.xs }}>
      <Ionicons name={icon} size={18} color={theme.primary} />
      <AppText variant="h2">{value}</AppText>
      <AppText variant="caption" color="textSecondary">
        {label}
      </AppText>
    </View>
  );
}
