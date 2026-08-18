import { Pressable, View, type ViewProps } from 'react-native';

import { Radius, Shadow, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export type CardProps = ViewProps & {
  /** surface(흰 카드) | muted(연한 샌드) | flat(테두리만) */
  tone?: 'surface' | 'muted' | 'flat';
  padding?: keyof typeof Spacing | 0;
  radius?: keyof typeof Radius;
  shadow?: keyof typeof Shadow;
  onPress?: () => void;
};

export function Card({
  tone = 'surface',
  padding = 'lg',
  radius = 'lg',
  shadow = 'sm',
  style,
  onPress,
  children,
  ...rest
}: CardProps) {
  const theme = useTheme();
  const base = [
    {
      backgroundColor: tone === 'muted' ? theme.surfaceMuted : theme.surface,
      borderRadius: Radius[radius],
      padding: padding === 0 ? 0 : Spacing[padding],
    },
    tone === 'flat' && { borderWidth: 1, borderColor: theme.border, backgroundColor: 'transparent' },
    tone !== 'flat' && Shadow[shadow],
    style,
  ];

  if (onPress) {
    return (
      <Pressable
        onPress={onPress}
        style={({ pressed }) => [base, pressed && { opacity: 0.9 }]}
        {...rest}>
        {children}
      </Pressable>
    );
  }
  return (
    <View style={base} {...rest}>
      {children}
    </View>
  );
}
