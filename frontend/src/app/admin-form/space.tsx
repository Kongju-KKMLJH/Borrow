import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';

import { ChipGroup, TextField, ToggleRow } from '@/components/form';
import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Screen } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';
import { adminApi } from '@/lib/api';
import type { ActivityField, DayOfWeek, FacilityType } from '@/lib/api/types';
import { ActivityFieldLabel, DayOfWeekLabel, FacilityTypeLabel } from '@/lib/format';

/**
 * 관리자 공간 생성·수정 (기능명세 7.3.2).
 *
 * 이용 가능 시간을 함께 받는다 — AI 매칭이 슬롯 시간 겹침으로 후보를 거르므로,
 * 슬롯 없는 공간은 추천에 걸리지 않는다.
 */

const FACILITY_OPTIONS: { label: string; value: FacilityType }[] =
  (['TABLE', 'LIGHTING', 'NATURAL_LIGHT', 'WATER', 'OUTLET', 'WIFI'] as FacilityType[])
    .map((f) => ({ label: FacilityTypeLabel[f], value: f }));

const FIELD_OPTIONS: { label: string; value: ActivityField }[] =
  (['ART', 'PHOTO'] as ActivityField[]).map((f) => ({ label: ActivityFieldLabel[f], value: f }));

const DAY_OPTIONS: { label: string; value: DayOfWeek }[] =
  (['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as DayOfWeek[])
    .map((d) => ({ label: DayOfWeekLabel[d], value: d }));

export default function AdminSpaceForm() {
  const theme = useTheme();
  const { spaceId } = useLocalSearchParams<{ spaceId?: string }>();
  const editingId = spaceId ? Number(spaceId) : null;

  const [ownerId, setOwnerId] = useState('');
  const [name, setName] = useState('');
  const [region, setRegion] = useState('');
  const [address, setAddress] = useState('');
  const [capacity, setCapacity] = useState('10');
  const [hourlyFee, setHourlyFee] = useState('10000');
  const [conditions, setConditions] = useState('');
  const [facilities, setFacilities] = useState<FacilityType[]>([]);
  const [allowedFields, setAllowedFields] = useState<ActivityField[]>(['ART']);
  const [noiseAllowed, setNoiseAllowed] = useState(true);
  const [messAllowed, setMessAllowed] = useState(true);
  const [days, setDays] = useState<DayOfWeek[]>(['SATURDAY']);
  const [slotStart, setSlotStart] = useState('10:00');
  const [slotEnd, setSlotEnd] = useState('18:00');
  const [loaded, setLoaded] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useAsync(async () => {
    if (editingId === null || loaded) return null;
    const found = (await adminApi.spaces()).find((s) => s.id === editingId);
    if (found) {
      setOwnerId(found.ownerId);
      setName(found.name);
      setRegion(found.region);
      setAddress(found.address ?? '');
      setCapacity(String(found.capacity));
      setHourlyFee(String(found.hourlyFee));
    }
    setLoaded(true);
    return found ?? null;
  }, [editingId, loaded]);

  function toggle<T extends string>(list: T[], value: T, set: (v: T[]) => void) {
    set(list.includes(value) ? list.filter((v) => v !== value) : [...list, value]);
  }

  async function save() {
    setBusy(true);
    setError(null);
    try {
      const body = {
        ownerId: ownerId.trim(),
        name: name.trim(),
        region: region.trim(),
        address: address.trim() || undefined,
        capacity: Number(capacity) || 0,
        hourlyFee: Number(hourlyFee) || 0,
        conditions: conditions.trim() || undefined,
        facilities,
        allowedFields,
        noiseAllowed,
        messAllowed,
        // 선택한 요일마다 같은 시간대를 깐다 — 시연 데이터를 만드는 데는 이 정도로 충분하다.
        slots: days.map((dayOfWeek) => ({ dayOfWeek, startTime: slotStart, endTime: slotEnd })),
      };
      if (editingId !== null) await adminApi.updateSpace(editingId, body);
      else await adminApi.createSpace(body);
      router.back();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '저장에 실패했어요.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen
      header={<ScreenHeader title={editingId !== null ? '공간 수정' : '공간 생성'} />}
      contentContainerStyle={{ gap: Spacing.lg, paddingHorizontal: Spacing.xl, paddingBottom: Spacing.huge }}>
      {error ? <AppText variant="caption" tint={theme.danger}>{error}</AppText> : null}

      <TextField label="등록자 아이디" value={ownerId} onChangeText={setOwnerId} placeholder="공간 파트너 계정" />
      <TextField label="공간명" value={name} onChangeText={setName} />
      <TextField label="지역" value={region} onChangeText={setRegion} placeholder="천안시 서북구 불당동" />
      <TextField label="주소" value={address} onChangeText={setAddress} />
      <TextField label="수용 인원" value={capacity} onChangeText={setCapacity} keyboardType="numeric" />
      <TextField label="시간당 이용료" value={hourlyFee} onChangeText={setHourlyFee} keyboardType="numeric" />
      <TextField label="이용 조건" value={conditions} onChangeText={setConditions} multiline />

      <ChipGroup
        label="보유 시설"
        options={FACILITY_OPTIONS}
        selected={facilities}
        onToggle={(v) => toggle(facilities, v as FacilityType, setFacilities)}
      />
      <ChipGroup
        label="허용 활동"
        options={FIELD_OPTIONS}
        selected={allowedFields}
        onToggle={(v) => toggle(allowedFields, v as ActivityField, setAllowedFields)}
      />

      <ToggleRow label="소음 허용" value={noiseAllowed} onValueChange={setNoiseAllowed} divider />
      <ToggleRow label="오염 허용" value={messAllowed} onValueChange={setMessAllowed} />

      <View style={{ gap: Spacing.sm }}>
        <ChipGroup
          label="이용 가능 요일"
          options={DAY_OPTIONS}
          selected={days}
          onToggle={(v) => toggle(days, v as DayOfWeek, setDays)}
        />
        <AppText variant="caption" color="textMuted">
          선택한 요일에 아래 시간대가 똑같이 만들어져요. AI 추천은 이 시간대로 걸러집니다.
        </AppText>
      </View>
      <TextField label="시작 시각" value={slotStart} onChangeText={setSlotStart} placeholder="10:00" />
      <TextField label="종료 시각" value={slotEnd} onChangeText={setSlotEnd} placeholder="18:00" />

      <View style={{ marginTop: Spacing.md }}>
        <Button label="저장" fullWidth loading={busy} onPress={save} />
      </View>
    </Screen>
  );
}
