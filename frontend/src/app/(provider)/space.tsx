import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { spaceApi } from '@/lib/api';
import { ChipGroup, Field, TextField, ToggleRow } from '@/components/form';
import { DialField } from '@/components/date-time-field';
import { ImageUploadField } from '@/components/image-upload-field';
import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Card } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import type { ActivityField, DayOfWeek, FacilityType, SpaceSlotResponse } from '@/lib/api/types';
import { DayOfWeekLabel, FacilityTypeLabel } from '@/lib/format';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

const FACILITY_OPTIONS = (Object.keys(FacilityTypeLabel) as FacilityType[]).map((v) => ({ label: FacilityTypeLabel[v], value: v }));
const DAYS = (Object.keys(DayOfWeekLabel) as DayOfWeek[]);

export default function ProviderSpace() {
  const theme = useTheme();
  const { data: spaces } = useAsync(() => spaceApi.list(), []);
  const existing = spaces?.[0];

  const [spaceId, setSpaceId] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const [address, setAddress] = useState('');
  const [conditions, setConditions] = useState('');
  const [capacity, setCapacity] = useState('12');
  const [region, setRegion] = useState('천안');
  const [hourlyFee, setHourlyFee] = useState('15000');
  const [facilities, setFacilities] = useState<FacilityType[]>(['TABLE', 'NATURAL_LIGHT', 'OUTLET']);
  const [allowedFields, setAllowedFields] = useState<ActivityField[]>(['ART', 'PHOTO']);
  const [noiseAllowed, setNoiseAllowed] = useState(false);
  const [messAllowed, setMessAllowed] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!existing) return;
    setSpaceId(existing.id);
    setName(existing.name);
    setImageUrls(existing.imageUrls ?? []);
    setAddress(existing.address ?? '');
    setConditions(existing.conditions ?? '');
    setCapacity(String(existing.capacity));
    setRegion(existing.region);
    setHourlyFee(String(existing.hourlyFee));
    setFacilities(existing.facilities);
    setAllowedFields(existing.allowedFields);
    setNoiseAllowed(existing.noiseAllowed);
    setMessAllowed(existing.messAllowed);
  }, [existing]);

  const toggleArr = <T,>(arr: T[], v: T, set: (a: T[]) => void) =>
    set(arr.includes(v) ? arr.filter((x) => x !== v) : [...arr, v]);

  const save = async () => {
    setSaving(true);
    try {
      const payload = {
        name, region, address: address || null, imageUrls,
        capacity: Number(capacity) || 0,
        hourlyFee: Number(hourlyFee) || 0, conditions: conditions || null,
        facilities, allowedFields, noiseAllowed, messAllowed,
      };
      const saved = spaceId ? await spaceApi.update(spaceId, payload) : await spaceApi.create(payload);
      setSpaceId(saved.id);
      router.back();
    } finally {
      setSaving(false);
    }
  };

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="공간 관리" />
      <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.xxl, paddingBottom: Spacing.huge }} showsVerticalScrollIndicator={false}>
        {/* 기본 정보 */}
        <Group title="기본 정보">
          <ImageUploadField label="공간 사진" value={imageUrls} onChange={setImageUrls} max={5} />
          <TextField label="공간명" value={name} onChangeText={setName} placeholder="브루 랩 카페" />
          <TextField label="지역" value={region} onChangeText={setRegion} placeholder="천안 서북구" />
          <TextField label="공간 주소" value={address} onChangeText={setAddress} placeholder="천안 서북구 두정동 100" />
          <TextField label="이용 조건" value={conditions} onChangeText={setConditions} placeholder="음료 1잔 주문 필수 등" multiline />
          <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
            <View style={{ flex: 1 }}><TextField label="최대 수용 인원" value={capacity} onChangeText={setCapacity} keyboardType="numeric" /></View>
            <View style={{ flex: 1 }}><TextField label="시간당 이용료" value={hourlyFee} onChangeText={setHourlyFee} keyboardType="numeric" /></View>
          </View>
        </Group>

        {/* 제공 시설 */}
        <Group title="제공 시설">
          <ChipGroup options={FACILITY_OPTIONS} selected={facilities} onToggle={(v) => toggleArr(facilities, v as FacilityType, setFacilities)} />
        </Group>

        {/* 허용 활동 */}
        <Group title="허용 활동">
          <Card tone="flat" padding="lg" radius="md" style={{ paddingVertical: 0 }}>
            <ToggleRow label="그림 활동 허용" value={allowedFields.includes('ART')} onValueChange={() => toggleArr(allowedFields, 'ART', setAllowedFields)} divider />
            <ToggleRow label="촬영 활동 허용" value={allowedFields.includes('PHOTO')} onValueChange={() => toggleArr(allowedFields, 'PHOTO', setAllowedFields)} divider />
            <ToggleRow label="소음 발생 활동 허용" value={noiseAllowed} onValueChange={setNoiseAllowed} divider />
            <ToggleRow label="오염(물감 등) 가능 활동 허용" value={messAllowed} onValueChange={setMessAllowed} />
          </Card>
        </Group>

        {/* 유휴시간 */}
        {spaceId ? (
          <SlotManager spaceId={spaceId} />
        ) : (
          <Group title="제공 가능 시간">
            <AppText variant="caption" color="textMuted">공간을 먼저 저장하면 유휴시간을 등록할 수 있어요.</AppText>
          </Group>
        )}
      </ScrollView>

      <View style={{ padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        <Button label={saving ? '' : '공간 정보 저장'} loading={saving} fullWidth onPress={save} />
      </View>
    </SafeAreaView>
  );
}

function SlotManager({ spaceId }: { spaceId: number }) {
  const theme = useTheme();
  const { data } = useAsync(() => spaceApi.slots(spaceId), [spaceId]);
  const [slots, setSlots] = useState<SpaceSlotResponse[]>([]);
  const [days, setDays] = useState<DayOfWeek[]>(['MONDAY']);
  const [start, setStart] = useState('14:00');
  const [end, setEnd] = useState('17:00');
  const [saving, setSaving] = useState(false);
  const [editTarget, setEditTarget] = useState<SpaceSlotResponse | null>(null);

  useEffect(() => { if (data) setSlots(sortSlots(data)); }, [data]);

  const isEditing = editTarget !== null;

  const toggleDay = (d: DayOfWeek) =>
    setDays((arr) => (arr.includes(d) ? arr.filter((x) => x !== d) : [...arr, d]));

  const startEdit = (slot: SpaceSlotResponse) => {
    setEditTarget(slot);
    setDays([slot.dayOfWeek]);
    setStart(slot.startTime.slice(0, 5));
    setEnd(slot.endTime.slice(0, 5));
  };

  const cancelEdit = () => {
    setEditTarget(null);
    setDays(['MONDAY']);
    setStart('14:00');
    setEnd('17:00');
  };

  const save = async () => {
    if (isEditing && editTarget) {
      setSaving(true);
      try {
        const updated = await spaceApi.updateSlot(spaceId, editTarget.id, {
          dayOfWeek: editTarget.dayOfWeek,
          startTime: start,
          endTime: end,
        });
        setSlots((s) => sortSlots(s.map((x) => (x.id === editTarget.id ? updated : x))));
        cancelEdit();
      } finally {
        setSaving(false);
      }
      return;
    }
    if (days.length === 0) return;
    // 선택한 요일 중 같은 시간대가 이미 등록된 요일은 건너뛴다.
    const targets = days.filter((d) => !slots.some((s) => s.dayOfWeek === d && s.startTime.slice(0, 5) === start && s.endTime.slice(0, 5) === end));
    if (targets.length === 0) return;
    setSaving(true);
    try {
      const created = await Promise.all(
        targets.map((d) => spaceApi.addSlot(spaceId, { dayOfWeek: d, startTime: start, endTime: end })),
      );
      setSlots((s) => sortSlots([...s, ...created]));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (slotId: number) => {
    if (editTarget?.id === slotId) cancelEdit();
    setSlots((s) => s.filter((x) => x.id !== slotId));
    await spaceApi.deleteSlot(spaceId, slotId);
  };

  return (
    <Group title="제공 가능 시간">
      {slots.length > 0 && (
        <Card tone="flat" padding="lg" radius="md" style={{ gap: 0 }}>
          {slots.map((s, i) => (
            <View key={s.id} style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, paddingVertical: Spacing.md, borderBottomWidth: i === slots.length - 1 ? 0 : 1, borderBottomColor: theme.border }}>
              <View style={{ width: 30, height: 30, borderRadius: 15, backgroundColor: theme.primarySoft, alignItems: 'center', justifyContent: 'center' }}>
                <AppText variant="small" tint={theme.primary}>{DayOfWeekLabel[s.dayOfWeek]}</AppText>
              </View>
              <AppText variant="body" style={{ flex: 1 }}>{s.startTime.slice(0, 5)} ~ {s.endTime.slice(0, 5)}</AppText>
              <Ionicons name="create-outline" size={18} color={theme.textSecondary} onPress={() => startEdit(s)} />
              <Ionicons name="trash-outline" size={18} color={theme.danger} onPress={() => remove(s.id)} />
            </View>
          ))}
        </Card>
      )}

      <Field label={isEditing ? '수정할 슬롯 (요일 고정)' : '요일 (복수 선택 가능)'}>
        <View style={{ flexDirection: 'row', gap: 6 }}>
          {DAYS.map((d) => (
            <View key={d} style={{ flex: 1 }} pointerEvents={isEditing ? 'none' : 'auto'}>
              <DayChip label={DayOfWeekLabel[d]} active={days.includes(d)} onPress={() => toggleDay(d)} />
            </View>
          ))}
        </View>
      </Field>
      <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
        <View style={{ flex: 1 }}><DialField label="시작 시간" mode="time" value={start} onChange={setStart} /></View>
        <View style={{ flex: 1 }}><DialField label="종료 시간" mode="time" value={end} onChange={setEnd} /></View>
      </View>
      <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
        {isEditing && (
          <Button label="취소" variant="outline" fullWidth onPress={cancelEdit} style={{ flex: 1 }} />
        )}
        <Button
          label={saving ? '' : isEditing ? '수정 저장' : days.length > 1 ? `${days.length}개 요일에 시간대 추가` : '시간대 추가'}
          loading={saving}
          disabled={!isEditing && days.length === 0}
          variant="outline"
          fullWidth
          onPress={save}
          style={{ flex: 1 }}
        />
      </View>
    </Group>
  );
}

const DAY_ORDER: Record<DayOfWeek, number> = { MONDAY: 0, TUESDAY: 1, WEDNESDAY: 2, THURSDAY: 3, FRIDAY: 4, SATURDAY: 5, SUNDAY: 6 };
function sortSlots(list: SpaceSlotResponse[]): SpaceSlotResponse[] {
  return [...list].sort((a, b) => DAY_ORDER[a.dayOfWeek] - DAY_ORDER[b.dayOfWeek] || a.startTime.localeCompare(b.startTime));
}

function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={{ gap: Spacing.md }}>
      <AppText variant="h3">{title}</AppText>
      {children}
    </View>
  );
}

function DayChip({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  const theme = useTheme();
  return (
    <Pressable onPress={onPress} style={{ alignItems: 'center', paddingVertical: 10, borderRadius: Radius.md, backgroundColor: active ? theme.text : theme.surfaceMuted, borderWidth: 1, borderColor: active ? theme.text : theme.border }}>
      <AppText variant="small" tint={active ? theme.textInverse : theme.textSecondary}>{label}</AppText>
    </Pressable>
  );
}
