import { Ionicons } from '@expo/vector-icons';
import { ActivityIndicator, Pressable, View, type StyleProp, type ViewStyle } from 'react-native';

import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

import { AppText } from './app-text';

type Variant = 'primary' | 'secondary' | 'outline' | 'ghost';
type Size = 'sm' | 'md' | 'lg';

export type ButtonProps = {
  label: string;
  onPress?: () => void;
  variant?: Variant;
  size?: Size;
  icon?: keyof typeof Ionicons.glyphMap;
  iconPosition?: 'left' | 'right';
  fullWidth?: boolean;
  disabled?: boolean;
  loading?: boolean;
  style?: StyleProp<ViewStyle>;
};

const SIZE = {
  sm: { pv: Spacing.sm, ph: Spacing.md, font: 'caption' as const, icon: 16 },
  md: { pv: Spacing.md, ph: Spacing.lg, font: 'title' as const, icon: 18 },
  lg: { pv: Spacing.lg, ph: Spacing.xl, font: 'h3' as const, icon: 20 },
};

export function Button({
  label,
  onPress,
  variant = 'primary',
  size = 'md',
  icon,
  iconPosition = 'left',
  fullWidth,
  disabled,
  loading,
  style,
}: ButtonProps) {
  const theme = useTheme();
  const s = SIZE[size];

  const bg: Record<Variant, string> = {
    primary: theme.primary,
    secondary: theme.secondary,
    outline: 'transparent',
    ghost: 'transparent',
  };
  const pressedBg: Record<Variant, string> = {
    primary: theme.primaryPressed,
    secondary: theme.secondaryPressed,
    outline: theme.surfaceMuted,
    ghost: theme.surfaceMuted,
  };
  const fg =
    variant === 'primary' || variant === 'secondary'
      ? theme.textInverse
      : variant === 'outline'
        ? theme.text
        : theme.primary;

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: Spacing.sm,
          paddingVertical: s.pv,
          paddingHorizontal: s.ph,
          borderRadius: Radius.md,
          backgroundColor: pressed ? pressedBg[variant] : bg[variant],
          borderWidth: variant === 'outline' ? 1 : 0,
          borderColor: theme.border,
          opacity: disabled ? 0.5 : 1,
        },
        fullWidth && { alignSelf: 'stretch' },
        style,
      ]}>
      {loading ? (
        <ActivityIndicator color={fg} size="small" />
      ) : (
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
          {icon && iconPosition === 'left' && <Ionicons name={icon} size={s.icon} color={fg} />}
          <AppText variant={s.font} tint={fg}>
            {label}
          </AppText>
          {icon && iconPosition === 'right' && <Ionicons name={icon} size={s.icon} color={fg} />}
        </View>
      )}
    </Pressable>
  );
}
