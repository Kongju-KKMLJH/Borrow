import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';

import { ConfirmModal } from '@/components/confirm-modal';
import { AppText, Button, Card, Screen } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';
import { adminApi } from '@/lib/api';
import type { AdminSpaceResponse } from '@/lib/api/types';
import { formatDateOnly } from '@/lib/admin-format';
import { formatCurrency } from '@/lib/format';

/** 기능명세 7.3 공간 관리 — 전체 목록 조회와 수정·삭제 */

export default function AdminSpaces() {
  const theme = useTheme();
  const [deleteTarget, setDeleteTarget] = useState<AdminSpaceResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: spaces, refetch } = useAsync(() => adminApi.spaces(), [], { refetchOnFocus: true });
  const list = spaces ?? [];

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
            <AppText variant="h2">공간 관리</AppText>
            <Button
              label="공간 생성"
              size="sm"
              icon="add"
              onPress={() => router.push('/admin-form/space')}
            />
          </View>
        }
        contentContainerStyle={{ gap: Spacing.lg, paddingBottom: Spacing.huge }}>
        {error ? (
          <View style={{ paddingHorizontal: Spacing.xl }}>
            <AppText variant="caption" tint={theme.danger}>{error}</AppText>
          </View>
        ) : null}

        <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
          {list.map((s) => (
            <SpaceRow
              key={s.id}
              space={s}
              onEdit={() => router.push({ pathname: '/admin-form/space', params: { spaceId: String(s.id) } })}
              onDelete={() => setDeleteTarget(s)}
            />
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
        visible={deleteTarget !== null}
        title="공간을 삭제할까요?"
        target={deleteTarget ? `${deleteTarget.name} (${deleteTarget.region})` : undefined}
        message="이용 가능 시간과 개최 요청이 함께 삭제되고, 이 공간에서 승인됐던 프로그램은 거절 상태로 되돌아갑니다. 되돌릴 수 없습니다."
        confirmLabel="삭제"
        loading={busy}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={async () => {
          const space = deleteTarget;
          if (!space) return;
          setBusy(true);
          setError(null);
          try {
            await adminApi.deleteSpace(space.id);
            setDeleteTarget(null);
            refetch();
          } catch (e: unknown) {
            setError(e instanceof Error ? e.message : '처리 중 오류가 발생했어요.');
          } finally {
            setBusy(false);
          }
        }}
      />
    </>
  );
}

function SpaceRow({
  space,
  onEdit,
  onDelete,
}: {
  space: AdminSpaceResponse;
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <Card tone="flat" padding="md" radius="lg" style={{ gap: Spacing.sm }}>
      <View style={{ gap: Spacing.sm }}>
        <View style={{ gap: 3 }}>
          <AppText variant="title" numberOfLines={1}>{space.name}</AppText>
          <AppText variant="caption" color="textMuted">
            id {space.id} · {space.ownerId} · 등록 {formatDateOnly(space.createdAt)}
          </AppText>
          {/* 관리자에게만 주소 전문을 준다 (공개 응답은 동 단위까지) */}
          <AppText variant="caption" color="textMuted" numberOfLines={1}>
            {space.address ?? space.region}
          </AppText>
          <AppText variant="caption" color="textMuted">
            최대 {space.capacity}명 · 시간당 {formatCurrency(space.hourlyFee)}
          </AppText>
        </View>
        <View style={{ flexDirection: 'row', gap: Spacing.xs }}>
          <Button label="수정" variant="outline" size="sm" onPress={onEdit} />
          <Button label="삭제" variant="outline" size="sm" onPress={onDelete} />
        </View>
      </View>
    </Card>
  );
}
