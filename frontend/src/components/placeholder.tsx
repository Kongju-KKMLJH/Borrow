import { Ionicons } from '@expo/vector-icons';
import { View, type ViewStyle } from 'react-native';

import { Radius } from '@/constants/theme';
import type { FieldType } from '@/data/types';
import { useTheme } from '@/hooks/use-theme';

const FIELD_ICON: Record<FieldType, keyof typeof Ionicons.glyphMap> = {
  drawing: 'color-palette',
  photo: 'camera',
};

/**
 * 이미지 자리 플레이스홀더 — 회색 블록 + 분야 아이콘 (Figma 디자인과 동일).
 * children으로 배지 등을 오버레이할 수 있다.
 */
export function ImagePlaceholder({
  field,
  height,
  radius = 'lg',
  style,
  iconSize,
  children,
}: {
  field?: FieldType;
  height: number;
  radius?: keyof typeof Radius;
  style?: ViewStyle;
  iconSize?: number;
  children?: React.ReactNode;
}) {
  const theme = useTheme();
  return (
    <View
      style={[
        { height, borderRadius: Radius[radius], backgroundColor: theme.surfaceDeep, overflow: 'hidden', alignItems: 'center', justifyContent: 'center' },
        style,
      ]}>
      <Ionicons name={FIELD_ICON[field ?? 'drawing']} size={iconSize ?? Math.min(height * 0.32, 48)} color={theme.textMuted} />
      {children}
    </View>
  );
}
