import { Ionicons } from '@expo/vector-icons';
import { Pressable, Switch, TextInput, View } from 'react-native';

import { AppText } from '@/components/ui';
import { FontFamily, Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <View style={{ gap: Spacing.sm }}>
      <AppText variant="label" color="textSecondary">{label}</AppText>
      {children}
    </View>
  );
}

export function TextField({
  label, value, onChangeText, placeholder, multiline, keyboardType,
}: {
  label: string; value?: string; onChangeText?: (t: string) => void; placeholder?: string; multiline?: boolean; keyboardType?: 'default' | 'numeric';
}) {
  const theme = useTheme();
  return (
    <Field label={label}>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={theme.textMuted}
        multiline={multiline}
        keyboardType={keyboardType}
        style={{
          backgroundColor: theme.surfaceMuted,
          borderRadius: Radius.md,
          paddingHorizontal: 14,
          paddingVertical: 13,
          fontFamily: FontFamily.regular,
          fontSize: 15,
          color: theme.text,
          minHeight: multiline ? 88 : undefined,
          textAlignVertical: multiline ? 'top' : 'center',
        }}
      />
    </Field>
  );
}

export function SelectField({ label, value, onPress }: { label: string; value: string; onPress?: () => void }) {
  const theme = useTheme();
  return (
    <Field label={label}>
      <Pressable
        onPress={onPress}
        style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: theme.surfaceMuted, borderRadius: Radius.md, paddingHorizontal: 14, paddingVertical: 13 }}>
        <AppText variant="body">{value}</AppText>
        <Ionicons name="chevron-down" size={18} color={theme.textMuted} />
      </Pressable>
    </Field>
  );
}

export type Option = { label: string; value: string };

export function ChipGroup({
  label, options, selected, onToggle,
}: {
  label?: string; options: Option[]; selected: string[]; onToggle: (value: string) => void;
}) {
  const inner = (
    <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.sm }}>
      {options.map((o) => (
        <SelectChip key={o.value} label={o.label} active={selected.includes(o.value)} onPress={() => onToggle(o.value)} />
      ))}
    </View>
  );
  return label ? <Field label={label}>{inner}</Field> : inner;
}

export function SelectChip({ label, active, onPress }: { label: string; active?: boolean; onPress?: () => void }) {
  const theme = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={{
        paddingHorizontal: 14, paddingVertical: 9, borderRadius: Radius.full,
        backgroundColor: active ? theme.text : theme.surfaceMuted,
        borderWidth: 1, borderColor: active ? theme.text : theme.border,
      }}>
      <AppText variant="small" tint={active ? theme.textInverse : theme.textSecondary}>{label}</AppText>
    </Pressable>
  );
}

export function ToggleRow({ label, value, onValueChange, divider }: { label: string; value: boolean; onValueChange: (v: boolean) => void; divider?: boolean }) {
  const theme = useTheme();
  return (
    <View
      style={{
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 12,
        borderBottomWidth: divider ? 1 : 0, borderBottomColor: theme.border,
      }}>
      <AppText variant="body">{label}</AppText>
      <Switch
        value={value}
        onValueChange={onValueChange}
        trackColor={{ false: theme.surfaceDeep, true: theme.primary }}
        thumbColor="#FFFFFF"
        ios_backgroundColor={theme.surfaceDeep}
      />
    </View>
  );
}

/** 만들기 위저드 진행 표시 */
export function Stepper({ current, total, label }: { current: number; total: number; label: string }) {
  const theme = useTheme();
  return (
    <View style={{ gap: Spacing.sm }}>
      <AppText variant="small" tint={theme.primary}>STEP {current} / {total} · {label}</AppText>
      <View style={{ flexDirection: 'row', gap: 4 }}>
        {Array.from({ length: total }).map((_, i) => (
          <View key={i} style={{ flex: 1, height: 4, borderRadius: Radius.full, backgroundColor: i < current ? theme.primary : theme.surfaceDeep }} />
        ))}
      </View>
    </View>
  );
}
