import { Ionicons } from '@expo/vector-icons';
import DateTimePicker, { type DateTimePickerEvent } from '@react-native-community/datetimepicker';
import { useState } from 'react';
import { Modal, Platform, Pressable, View } from 'react-native';

import { Field, TextField } from '@/components/form';
import { AppText, Button } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

function pad(n: number) {
  return String(n).padStart(2, '0');
}

/**
 * 날짜/시간 선택 필드. 탭하면 팝업이 뜬다.
 * - iOS: 바텀시트 모달 — 날짜는 인라인 캘린더, 시간은 전폭 휠 스피너(반폭 컬럼 잘림 방지).
 * - Android: 네이티브 다이얼로그(캘린더/시계).
 * - 웹: 스피너 미지원 → 포맷 힌트가 있는 텍스트 입력으로 폴백.
 *
 * value 포맷 — date: "YYYY-MM-DD", time: "HH:mm" (백엔드 LocalDate/LocalTime와 동일).
 */
export function DialField({
  label,
  mode,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  mode: 'date' | 'time';
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
}) {
  const theme = useTheme();
  const [show, setShow] = useState(false);
  const [draft, setDraft] = useState<Date | null>(null);

  // 웹 폴백: 텍스트 입력
  if (Platform.OS === 'web') {
    return (
      <TextField
        label={label}
        value={value}
        onChangeText={onChange}
        placeholder={placeholder ?? (mode === 'date' ? '2026-08-01' : '14:00')}
      />
    );
  }

  const toDate = (): Date => {
    const base = new Date();
    if (mode === 'date' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return new Date(`${value}T00:00:00`);
    }
    if (mode === 'time' && /^\d{2}:\d{2}$/.test(value)) {
      const [h, m] = value.split(':').map(Number);
      base.setHours(h, m, 0, 0);
      return base;
    }
    return base;
  };

  const fmt = (d: Date): string =>
    mode === 'date'
      ? `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
      : `${pad(d.getHours())}:${pad(d.getMinutes())}`;

  const displayText = (): string => {
    if (!value) return placeholder ?? (mode === 'date' ? '날짜 선택' : '시간 선택');
    const d = toDate();
    if (mode === 'date') return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;
    const period = d.getHours() < 12 ? '오전' : '오후';
    const h12 = d.getHours() % 12 === 0 ? 12 : d.getHours() % 12;
    return `${period} ${h12}:${pad(d.getMinutes())}`;
  };

  const open = () => {
    setDraft(toDate());
    setShow(true);
  };

  const trigger = (
    <Pressable
      onPress={open}
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: theme.surfaceMuted,
        borderRadius: Radius.md,
        paddingHorizontal: 14,
        paddingVertical: 13,
        borderWidth: 1,
        borderColor: show ? theme.primary : 'transparent',
      }}>
      <AppText variant="body" color={value ? 'text' : 'textMuted'}>
        {displayText()}
      </AppText>
      <Ionicons name={mode === 'date' ? 'calendar-outline' : 'time-outline'} size={18} color={theme.textMuted} />
    </Pressable>
  );

  // Android: 네이티브 다이얼로그 (자체적으로 모달로 뜨므로 잘림 없음)
  if (Platform.OS === 'android') {
    const handleChange = (event: DateTimePickerEvent, selected?: Date) => {
      setShow(false);
      if (event.type === 'dismissed' || !selected) return;
      onChange(fmt(selected));
    };
    return (
      <Field label={label}>
        {trigger}
        {show && (
          <DateTimePicker
            value={toDate()}
            mode={mode}
            display={mode === 'date' ? 'calendar' : 'clock'}
            minuteInterval={mode === 'time' ? 5 : undefined}
            minimumDate={mode === 'date' ? new Date() : undefined}
            onChange={handleChange}
          />
        )}
      </Field>
    );
  }

  // iOS: 바텀시트 모달 (전폭 → 스피너 잘림 없음, 날짜는 인라인 캘린더)
  const confirm = () => {
    if (draft) onChange(fmt(draft));
    setShow(false);
  };

  return (
    <Field label={label}>
      {trigger}
      <Modal visible={show} transparent animationType="slide" onRequestClose={() => setShow(false)}>
        <Pressable
          onPress={() => setShow(false)}
          style={{ flex: 1, backgroundColor: 'rgba(0,0,0,0.35)', justifyContent: 'flex-end' }}>
          <Pressable
            onPress={() => {}}
            style={{
              backgroundColor: theme.surface,
              borderTopLeftRadius: Radius.xl,
              borderTopRightRadius: Radius.xl,
              paddingHorizontal: Spacing.xl,
              paddingTop: Spacing.lg,
              paddingBottom: Spacing.xl,
              gap: Spacing.md,
            }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
              <AppText variant="h3">{label}</AppText>
              <Pressable onPress={() => setShow(false)} hitSlop={8}>
                <Ionicons name="close" size={22} color={theme.textMuted} />
              </Pressable>
            </View>
            <View style={{ alignItems: 'center' }}>
              <DateTimePicker
                value={draft ?? toDate()}
                mode={mode}
                display={mode === 'date' ? 'inline' : 'spinner'}
                minuteInterval={mode === 'time' ? 5 : undefined}
                minimumDate={mode === 'date' ? new Date() : undefined}
                onChange={(_, selected) => selected && setDraft(selected)}
                themeVariant="light"
                accentColor={theme.primary}
                style={{ alignSelf: 'stretch' }}
              />
            </View>
            <Button label="확인" fullWidth onPress={confirm} />
          </Pressable>
        </Pressable>
      </Modal>
    </Field>
  );
}
