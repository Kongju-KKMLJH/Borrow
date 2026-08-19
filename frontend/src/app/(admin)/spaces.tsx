import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { ScrollView, View } from 'react-native';

import { ConfirmModal } from '@/components/confirm-modal';
import { SelectChip } from '@/components/form';
import { AppText, Badge, Button, Card, Screen } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';
import { adminApi } from '@/lib/api';
import type { AdminSpaceResponse } from '@/lib/api/types';
import { formatDateOnly } from '@/lib/admin-format';
import { formatCurrency } from '@/lib/format';

/** 기능명세 7.3 공간 관리 — 전체 목록 조회와 강제 삭제 */

const FILTERS = [
  { label: '전체', match: () => true },
  { label: '운영 중', match: (s: AdminSpaceResponse) => !s.forceDeleted },
  { label: '삭제됨', match: (s: AdminSpaceResponse) => s.forceDeleted },
];

export default function AdminSpaces() {
  const theme = useTheme();
  const [idx, setIdx] = useState(0);
  const [target, setTarget] = useState<AdminSpaceResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: spaces, refetch } = useAsync(() => adminApi.spaces(), [], { refetchOnFocus: true });
  const list = (spaces ?? []).filter(FILTERS[idx].match);

  async function forceDelete(space: AdminSpaceResponse) {
    setBusy(true);
    setError(null);
    try {
      await adminApi.forceDeleteSpace(space.id);
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
          <View style={{ paddingHorizontal: Spacing.xl, paddingTop: Spacing.md, paddingBottom: Spacing.sm }}>
            <AppText variant="h2">공간 관리</AppText>
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
          {list.map((s) => (
            <SpaceRow key={s.id} space={s} onForceDelete={() => setTarget(s)} />
          ))}
          {list.length === 0 ? (
            <View style={{ alignItems: 'center', paddingVertical: Spacing.huge, gap: Spacing.sm }}>
              <Ionicons name="storefront-outline" size={32} color={theme.textMuted} />
              <AppText variant="body" color="textMuted">표시할 공간이 없어요</AppText>
            </View>
          ) : null}
        </View>
      </Screen>

      <ConfirmModal
        visible={target !== null}
        title="공간을 강제 삭제할까요?"
        target={target ? `${target.name} (${target.region})` : undefined}
        message="AI 추천과 개최 요청 대상에서 제외되고, 진행 중인 개최 요청은 모두 자동 거절됩니다."
        confirmLabel="강제 삭제"
        loading={busy}
        onCancel={() => setTarget(null)}
        onConfirm={() => target && forceDelete(target)}
      />
    </>
  );
}

function SpaceRow({ space, onForceDelete }: { space: AdminSpaceResponse; onForceDelete: () => void }) {
  return (
    <Card tone="flat" padding="md" radius="lg" style={{ gap: Spacing.sm }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
        <View style={{ flex: 1, gap: 3 }}>
          <AppText variant="title" numberOfLines={1}>{space.name}</AppText>
          <AppText variant="caption" color="textMuted">
            {space.ownerId} · 등록 {formatDateOnly(space.createdAt)}
          </AppText>
          {/* 관리자에게만 주소 전문을 준다 (공개 응답은 동 단위까지) */}
          <AppText variant="caption" color="textMuted" numberOfLines={1}>
            {space.address ?? space.region}
          </AppText>
          <AppText variant="caption" color="textMuted">
            최대 {space.capacity}명 · 시간당 {formatCurrency(space.hourlyFee)}
          </AppText>
        </View>
        {space.forceDeleted ? null : (
          <Button label="강제 삭제" variant="outline" size="sm" onPress={onForceDelete} />
        )}
      </View>

      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs }}>
        {space.mock ? <Badge label="임시 공간" tone="accent" /> : null}
        {space.forceDeleted ? <Badge label="삭제됨" tone="danger" /> : null}
      </View>
    </Card>
  );
}
