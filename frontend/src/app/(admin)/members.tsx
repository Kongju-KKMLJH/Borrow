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
import type { AdminUserResponse, AdminVerificationResponse, Role } from '@/lib/api/types';
import { RoleLabel, VerificationLabel, userSubtitle } from '@/lib/admin-format';

/** 기능명세 7.1 회원 관리 — 목록 조회, 강제 탈퇴, 예술가 인증 승인 */

const FILTERS: { label: string; value?: Role }[] = [
  { label: '전체' },
  { label: '시민', value: 'MEMBER' },
  { label: '예술가', value: 'ARTIST' },
  { label: '공간 파트너', value: 'HOST' },
];

export default function AdminMembers() {
  const theme = useTheme();
  const [idx, setIdx] = useState(0);
  const [target, setTarget] = useState<AdminUserResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminUserResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: users, refetch } = useAsync(() => adminApi.users(), [], { refetchOnFocus: true });
  const { data: pending, refetch: refetchPending } = useAsync(
    () => adminApi.verifications('PENDING'),
    [],
    { refetchOnFocus: true },
  );

  const role = FILTERS[idx].value;
  const list = (users ?? []).filter((u) => !role || u.role === role);

  async function run(action: () => Promise<unknown>, after: () => void) {
    setBusy(true);
    setError(null);
    try {
      await action();
      after();
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
            <AppText variant="h2">회원 관리</AppText>
            <Button
              label="임시 회원 생성"
              size="sm"
              icon="add"
              onPress={() => router.push('/admin-form/user')}
            />
          </View>
        }
        contentContainerStyle={{ gap: Spacing.lg, paddingBottom: Spacing.huge }}>
        {error ? (
          <View style={{ paddingHorizontal: Spacing.xl }}>
            <AppText variant="caption" tint={theme.danger}>{error}</AppText>
          </View>
        ) : null}

        {/* 예술가 인증 심사 (7.1.4) */}
        {pending && pending.length > 0 ? (
          <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
            <AppText variant="h3">인증 심사 대기 {pending.length}건</AppText>
            {pending.map((v) => (
              <VerificationRow
                key={v.id}
                verification={v}
                busy={busy}
                onApprove={() =>
                  run(() => adminApi.approveVerification(v.id), () => {
                    refetchPending();
                    refetch();
                  })
                }
              />
            ))}
          </View>
        ) : null}

        {/* 회원 목록 (7.1.1) */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={{ gap: Spacing.sm, paddingHorizontal: Spacing.xl }}>
          {FILTERS.map((f, i) => (
            <SelectChip key={f.label} label={f.label} active={idx === i} onPress={() => setIdx(i)} />
          ))}
        </ScrollView>

        <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
          {list.map((u) => (
            <MemberRow
              key={u.id}
              user={u}
              onWithdraw={() => setTarget(u)}
              onEdit={() => router.push({ pathname: '/admin-form/user', params: { userId: String(u.id) } })}
              onDelete={() => setDeleteTarget(u)}
            />
          ))}
          {list.length === 0 ? (
            <View style={{ alignItems: 'center', paddingVertical: Spacing.huge, gap: Spacing.sm }}>
              <Ionicons name="people-outline" size={32} color={theme.textMuted} />
              <AppText variant="body" color="textMuted">표시할 회원이 없어요</AppText>
            </View>
          ) : null}
        </View>
      </Screen>

      <ConfirmModal
        visible={deleteTarget !== null}
        title="임시 회원을 삭제할까요?"
        target={deleteTarget ? `${deleteTarget.nickname} (${deleteTarget.loginId})` : undefined}
        message="임시 회원 데이터가 목록에서 완전히 지워집니다. 이 회원이 남긴 프로그램·공간·참여가 있으면 삭제되지 않습니다."
        confirmLabel="삭제"
        loading={busy}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => {
          const user = deleteTarget;
          if (!user) return;
          run(() => adminApi.deleteUser(user.id), () => {
            setDeleteTarget(null);
            refetch();
          });
        }}
      />

      <ConfirmModal
        visible={target !== null}
        title="회원을 강제 탈퇴시킬까요?"
        target={target ? `${target.nickname} (${target.loginId})` : undefined}
        message="탈퇴 처리된 회원은 로그인과 서비스 이용이 차단됩니다. 기존 데이터는 삭제되지 않고 비활성 상태로 남습니다."
        confirmLabel="강제 탈퇴"
        loading={busy}
        onCancel={() => setTarget(null)}
        onConfirm={() => {
          const user = target;
          if (!user) return;
          run(() => adminApi.withdrawUser(user.id), () => {
            setTarget(null);
            refetch();
          });
        }}
      />
    </>
  );
}

function MemberRow({
  user,
  onWithdraw,
  onEdit,
  onDelete,
}: {
  user: AdminUserResponse;
  onWithdraw: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <Card tone="flat" padding="md" radius="lg" style={{ gap: Spacing.sm }}>
      <View style={{ gap: Spacing.sm }}>
        <View style={{ gap: 3 }}>
          <AppText variant="title">{user.nickname}</AppText>
          <AppText variant="caption" color="textMuted">{userSubtitle(user)}</AppText>
        </View>
        <View style={{ flexDirection: 'row', gap: Spacing.xs }}>
          {/* 임시 회원은 수정·삭제, 실제 회원은 강제 탈퇴 — 대상이 다르다 (7.1.2 vs 7.1.3) */}
          {user.mock ? (
            <>
              <Button label="수정" variant="outline" size="sm" onPress={onEdit} />
              <Button label="삭제" variant="outline" size="sm" onPress={onDelete} />
            </>
          ) : null}
          {user.withdrawn || user.role === 'ADMIN' ? null : (
            <Button label="강제 탈퇴" variant="outline" size="sm" onPress={onWithdraw} />
          )}
        </View>
      </View>

      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs }}>
        <Badge label={RoleLabel[user.role]} tone={user.role === 'ARTIST' ? 'secondary' : 'neutral'} />
        {user.role === 'ARTIST' ? (
          <Badge
            label={VerificationLabel[user.verificationStatus]}
            tone={user.verificationStatus === 'APPROVED' ? 'primary' : 'neutral'}
            icon={user.verificationStatus === 'APPROVED' ? 'ribbon' : undefined}
          />
        ) : null}
        {user.mock ? <Badge label="임시 회원" tone="accent" /> : null}
        {user.withdrawn ? <Badge label="탈퇴" tone="danger" /> : null}
      </View>
    </Card>
  );
}

function VerificationRow({
  verification,
  busy,
  onApprove,
}: {
  verification: AdminVerificationResponse;
  busy: boolean;
  onApprove: () => void;
}) {
  return (
    <Card tone="muted" padding="md" radius="lg" shadow="none" style={{ gap: Spacing.sm }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
        <View style={{ flex: 1, gap: 3 }}>
          <AppText variant="title">{verification.nickname ?? verification.loginId}</AppText>
          <AppText variant="caption" color="textMuted">{verification.loginId}</AppText>
        </View>
        <Button label="인증 승인" size="sm" disabled={busy} onPress={onApprove} />
      </View>
      <AppText variant="caption" color="textSecondary" numberOfLines={1}>
        {verification.portfolioUrl}
      </AppText>
      {verification.career ? (
        <AppText variant="caption" color="textMuted" numberOfLines={2}>
          {verification.career}
        </AppText>
      ) : null}
    </Card>
  );
}
