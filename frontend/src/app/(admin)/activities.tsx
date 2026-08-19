import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, View } from 'react-native';

import { ConfirmModal } from '@/components/confirm-modal';
import { SelectChip } from '@/components/form';
import { AppText, Badge, Button, Card, Screen } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';
import { adminApi } from '@/lib/api';
import type { AdminActivityResponse } from '@/lib/api/types';
import { activityStatusLabel } from '@/lib/admin-format';
import { ActivityTypeLabel, formatDateTime } from '@/lib/format';

/** 기능명세 7.2 프로그램 관리 — 전체 목록 조회와 강제 삭제 */

const FILTERS = [
  { label: '전체', match: () => true },
  { label: '모집 중', match: (a: AdminActivityResponse) => a.status === 'PUBLISHED' && !a.forceDeleted },
  { label: '승인 대기', match: (a: AdminActivityResponse) => a.status === 'PENDING' },
  { label: '삭제됨', match: (a: AdminActivityResponse) => a.forceDeleted },
];

export default function AdminActivities() {
  const theme = useTheme();
  const [idx, setIdx] = useState(0);
  const [target, setTarget] = useState<AdminActivityResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminActivityResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: activities, refetch } = useAsync(() => adminApi.activities(), [], { refetchOnFocus: true });
  const list = (activities ?? []).filter(FILTERS[idx].match);

  async function forceDelete(activity: AdminActivityResponse) {
    setBusy(true);
    setError(null);
    try {
      await adminApi.forceDeleteActivity(activity.id);
      setTarget(null);
      refetch();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '처리 중 오류가 발생했어요.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Screen
        header={
          <View
            style={{
              flexDirection: 'row',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingHorizontal: Spacing.xl,
              paddingTop: Spacing.md,
              paddingBottom: Spacing.sm,
            }}>
            <AppText variant="h2">프로그램 관리</AppText>
            <Button
              label="임시 생성"
              size="sm"
              icon="add"
              onPress={() => router.push('/admin-form/activity')}
            />
          </View>
        }
        contentContainerStyle={{ gap: Spacing.lg, paddingBottom: Spacing.huge }}>
        {error ? (
          <View style={{ paddingHorizontal: Spacing.xl }}>
            <AppText variant="caption" tint={theme.danger}>{error}</AppText>
          </View>
        ) : null}

        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={{ gap: Spacing.sm, paddingHorizontal: Spacing.xl }}>
          {FILTERS.map((f, i) => (
            <SelectChip key={f.label} label={f.label} active={idx === i} onPress={() => setIdx(i)} />
          ))}
        </ScrollView>

        <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
          {list.map((a) => (
            <ActivityRow
              key={a.id}
              activity={a}
              onForceDelete={() => setTarget(a)}
              onEdit={() => router.push({ pathname: '/admin-form/activity', params: { activityId: String(a.id) } })}
              onDelete={() => setDeleteTarget(a)}
            />
          ))}
          {list.length === 0 ? (
            <View style={{ alignItems: 'center', paddingVertical: Spacing.huge, gap: Spacing.sm }}>
              <Ionicons name="documents-outline" size={32} color={theme.textMuted} />
              <AppText variant="body" color="textMuted">표시할 프로그램이 없어요</AppText>
            </View>
          ) : null}
        </View>
      </Screen>

      <ConfirmModal
        visible={deleteTarget !== null}
        title="임시 프로그램을 삭제할까요?"
        target={deleteTarget?.title}
        message="임시 프로그램 데이터가 목록에서 완전히 지워집니다. 참여 신청이 있으면 삭제되지 않습니다."
        confirmLabel="삭제"
        loading={busy}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={async () => {
          const activity = deleteTarget;
          if (!activity) return;
          setBusy(true);
          setError(null);
          try {
            await adminApi.deleteActivity(activity.id);
            setDeleteTarget(null);
            refetch();
          } catch (e: unknown) {
            setError(e instanceof Error ? e.message : '처리 중 오류가 발생했어요.');
          } finally {
            setBusy(false);
          }
        }}
      />

      <ConfirmModal
        visible={target !== null}
        title="프로그램을 강제 삭제할까요?"
        target={target?.title}
        message="시민 탐색과 참여 신청 대상에서 제외됩니다. 기존 참여 신청 내역은 보존됩니다."
        confirmLabel="강제 삭제"
        loading={busy}
        onCancel={() => setTarget(null)}
        onConfirm={() => target && forceDelete(target)}
      />
    </>
  );
}

function ActivityRow({
  activity,
  onForceDelete,
  onEdit,
  onDelete,
}: {
  activity: AdminActivityResponse;
  onForceDelete: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <Card tone="flat" padding="md" radius="lg" style={{ gap: Spacing.sm }}>
      <View style={{ gap: Spacing.sm }}>
        <View style={{ gap: 3 }}>
          <AppText variant="title" numberOfLines={1}>{activity.title}</AppText>
          <AppText variant="caption" color="textMuted">
            id {activity.id} · {activity.hostNickname ?? activity.hostLoginId} · {activity.spaceName ?? '공간 미확정'}
          </AppText>
          <AppText variant="caption" color="textMuted">
            {formatDateTime(activity.date, activity.startTime, activity.endTime)} · 정원 {activity.capacity}명
          </AppText>
        </View>
        <View style={{ flexDirection: 'row', gap: Spacing.xs }}>
          {activity.mock ? (
            <>
              <Button label="수정" variant="outline" size="sm" onPress={onEdit} />
              <Button label="삭제" variant="outline" size="sm" onPress={onDelete} />
            </>
          ) : null}
          {activity.forceDeleted ? null : (
            <Button label="강제 삭제" variant="outline" size="sm" onPress={onForceDelete} />
          )}
        </View>
      </View>

      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs }}>
        <Badge label={activityStatusLabel(activity.status)} tone={activity.status === 'PUBLISHED' ? 'primary' : 'neutral'} />
        <Badge label={ActivityTypeLabel[activity.type]} tone="neutral" />
        {activity.mock ? <Badge label="임시 프로그램" tone="accent" /> : null}
        {activity.forceDeleted ? <Badge label="삭제됨" tone="danger" /> : null}
      </View>
    </Card>
  );
}
