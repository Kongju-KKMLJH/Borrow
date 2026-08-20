import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';

import { ChipGroup, TextField } from '@/components/form';
import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Screen } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';
import { adminApi } from '@/lib/api';
import type { ActivityField, ActivityStatus } from '@/lib/api/types';
import { activityStatusLabel } from '@/lib/admin-format';
import { ActivityFieldLabel } from '@/lib/format';

/**
 * 관리자 프로그램 생성·수정 (기능명세 7.2.2).
 *
 * 유형(HOBBY/CLASS)과 인증 배지는 여기서 고르지 않는다 — 담당 예술가 계정의 역할을 보고
 * 서버가 정한다. 클라이언트가 배지를 위조하지 못하게 하는 기존 규칙 그대로다.
 */

const FIELD_OPTIONS: { label: string; value: ActivityField }[] =
  (['ART', 'PHOTO'] as ActivityField[]).map((f) => ({ label: ActivityFieldLabel[f], value: f }));

const STATUS_VALUES = ['DRAFT', 'PENDING', 'MATCHED', 'PUBLISHED', 'REJECTED'] as const;
const STATUS_OPTIONS = STATUS_VALUES.map((s) => ({ label: activityStatusLabel(s as ActivityStatus), value: s }));

/** 공간이 있어야 도달할 수 있는 상태 (백엔드 상태 흐름과 같은 판정) */
const NEEDS_SPACE: string[] = ['MATCHED', 'PUBLISHED'];

export default function AdminActivityForm() {
  const theme = useTheme();
  const { activityId } = useLocalSearchParams<{ activityId?: string }>();
  const editingId = activityId ? Number(activityId) : null;

  const [hostLoginId, setHostLoginId] = useState('');
  const [spaceId, setSpaceId] = useState('');
  const [field, setField] = useState<ActivityField>('ART');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState('2026-12-12');
  const [startTime, setStartTime] = useState('14:00');
  const [endTime, setEndTime] = useState('16:00');
  const [capacity, setCapacity] = useState('5');
  const [entryFee, setEntryFee] = useState('10000');
  const [status, setStatus] = useState<string>('DRAFT');
  const [loaded, setLoaded] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useAsync(async () => {
    if (editingId === null || loaded) return null;
    const found = (await adminApi.activities()).find((a) => a.id === editingId);
    if (found) {
      setHostLoginId(found.hostLoginId);
      setSpaceId(found.spaceId !== null ? String(found.spaceId) : '');
      setTitle(found.title);
      setDate(found.date);
      setStartTime(found.startTime.slice(0, 5));
      setEndTime(found.endTime.slice(0, 5));
      setCapacity(String(found.capacity));
      setStatus(found.status);
    }
    setLoaded(true);
    return found ?? null;
  }, [editingId, loaded]);

  const spaceRequired = NEEDS_SPACE.includes(status);
  const missingSpace = spaceRequired && !spaceId.trim();

  async function save() {
    setBusy(true);
    setError(null);
    try {
      const body = {
        hostLoginId: hostLoginId.trim(),
        spaceId: spaceId.trim() ? Number(spaceId) : null,
        field,
        title: title.trim(),
        description: description.trim() || undefined,
        date: date.trim(),
        startTime: startTime.trim(),
        endTime: endTime.trim(),
        capacity: Number(capacity) || 0,
        entryFee: Number(entryFee) || 0,
        status: status as ActivityStatus,
      };
      if (editingId !== null) await adminApi.updateActivity(editingId, body);
      else await adminApi.createActivity(body);
      router.back();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '저장에 실패했어요.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen
      header={<ScreenHeader title={editingId !== null ? '프로그램 수정' : '프로그램 생성'} />}
      contentContainerStyle={{ gap: Spacing.lg, paddingHorizontal: Spacing.xl, paddingBottom: Spacing.huge }}>
      {error ? <AppText variant="caption" tint={theme.danger}>{error}</AppText> : null}

      <TextField
        label="담당 예술가 아이디"
        value={hostLoginId}
        onChangeText={setHostLoginId}
        placeholder="이 계정의 역할이 유형·인증 배지를 정합니다"
      />
      <TextField label="프로그램명" value={title} onChangeText={setTitle} />
      <TextField label="설명" value={description} onChangeText={setDescription} multiline />

      <ChipGroup
        label="분야"
        options={FIELD_OPTIONS}
        selected={[field]}
        onToggle={(v) => setField(v as ActivityField)}
      />

      <TextField label="날짜" value={date} onChangeText={setDate} placeholder="2026-12-12" />
      <TextField label="시작 시각" value={startTime} onChangeText={setStartTime} placeholder="14:00" />
      <TextField label="종료 시각" value={endTime} onChangeText={setEndTime} placeholder="16:00" />
      <TextField label="모집 정원" value={capacity} onChangeText={setCapacity} keyboardType="numeric" />
      <TextField label="참가비" value={entryFee} onChangeText={setEntryFee} keyboardType="numeric" />

      <ChipGroup label="상태" options={STATUS_OPTIONS} selected={[status]} onToggle={setStatus} />

      <View style={{ gap: Spacing.xs }}>
        <TextField
          label={spaceRequired ? '공간 id (필수)' : '공간 id (선택)'}
          value={spaceId}
          onChangeText={setSpaceId}
          keyboardType="numeric"
          placeholder="공간 관리 화면에서 확인"
        />
        {missingSpace ? (
          <AppText variant="caption" tint={theme.danger}>
            이 상태로 만들려면 공간이 필요해요.
          </AppText>
        ) : (
          <AppText variant="caption" color="textMuted">
            공간을 지정하면 개최 요청까지 함께 만들어집니다.
          </AppText>
        )}
      </View>

      <View style={{ marginTop: Spacing.md }}>
        <Button label="저장" fullWidth loading={busy} disabled={missingSpace} onPress={save} />
      </View>
    </Screen>
  );
}
