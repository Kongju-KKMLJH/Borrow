import { useLocalSearchParams, router } from 'expo-router';
import { useEffect, useState } from 'react';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { activityApi } from '@/lib/api';
import { TextField } from '@/components/form';
import { DialField } from '@/components/date-time-field';
import { ScreenHeader } from '@/components/nav';
import { AppText, Button } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import type { ActivityField } from '@/lib/api/types';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

export default function EditActivity() {
  const theme = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const activityId = Number(id);
  const { data: activity, loading } = useAsync(() => activityApi.detail(activityId), [activityId]);
  const [field, setField] = useState<ActivityField>('ART');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [capacity, setCapacity] = useState('');
  const [entryFee, setEntryFee] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!activity) return;
    setField(activity.field); setTitle(activity.title); setDescription(activity.description ?? '');
    setDate(activity.date); setStartTime(activity.startTime.slice(0, 5)); setEndTime(activity.endTime.slice(0, 5));
    setCapacity(String(activity.capacity)); setEntryFee(String(activity.entryFee));
  }, [activity]);

  const save = async () => {
    setSaving(true); setError(null);
    try {
      await activityApi.update(activityId, { field, title: title.trim(), description: description.trim() || null, imageUrls: activity?.imageUrls ?? null, date, startTime, endTime, capacity: Number(capacity), entryFee: Number(entryFee) });
      router.replace('/(user)/my-activities');
    } catch { setError('활동 수정에 실패했어요. 입력값을 확인해주세요.'); } finally { setSaving(false); }
  };

  if (loading || !activity) return <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }}><ScreenHeader title="활동 수정" /><View /></SafeAreaView>;
  return <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
    <ScreenHeader title="활동 수정" />
    <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.lg }}>
      <TextField label="분야 (ART / PHOTO)" value={field} onChangeText={(v) => setField(v === 'PHOTO' ? 'PHOTO' : 'ART')} />
      <TextField label="활동 제목" value={title} onChangeText={setTitle} />
      <TextField label="활동 설명" value={description} onChangeText={setDescription} multiline />
      <DialField label="활동 날짜" mode="date" value={date} onChange={setDate} />
      <View style={{ flexDirection: 'row', gap: Spacing.sm }}><View style={{ flex: 1 }}><DialField label="시작 시간" mode="time" value={startTime} onChange={setStartTime} /></View><View style={{ flex: 1 }}><DialField label="종료 시간" mode="time" value={endTime} onChange={setEndTime} /></View></View>
      <View style={{ flexDirection: 'row', gap: Spacing.sm }}><View style={{ flex: 1 }}><TextField label="모집 인원" value={capacity} onChangeText={setCapacity} keyboardType="numeric" /></View><View style={{ flex: 1 }}><TextField label="참가비" value={entryFee} onChangeText={setEntryFee} keyboardType="numeric" /></View></View>
      {error && <AppText variant="caption" tint={theme.danger}>{error}</AppText>}
    </ScrollView>
    <View style={{ padding: Spacing.xl }}><Button label="수정 저장" loading={saving} fullWidth onPress={save} /></View>
  </SafeAreaView>;
}
