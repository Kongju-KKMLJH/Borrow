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
import type { AdminVerificationLabel, Role } from '@/lib/api/types';
import { RoleLabel, VerificationLabel } from '@/lib/admin-format';

/**
 * 관리자 회원 생성·수정 (기능명세 7.1.2).
 * `userId` 파라미터가 있으면 수정, 없으면 생성이다.
 */

const ROLE_OPTIONS: { label: string; value: Role }[] = (['MEMBER', 'ARTIST', 'HOST'] as Role[])
  .map((r) => ({ label: RoleLabel[r], value: r }));

const VERIFICATION_OPTIONS: { label: string; value: AdminVerificationLabel }[] =
  (['NONE', 'PENDING', 'APPROVED', 'REJECTED'] as AdminVerificationLabel[])
    .map((v) => ({ label: VerificationLabel[v], value: v }));

export default function AdminUserForm() {
  const theme = useTheme();
  const { userId } = useLocalSearchParams<{ userId?: string }>();
  const editingId = userId ? Number(userId) : null;

  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [role, setRole] = useState<Role>('MEMBER');
  const [verification, setVerification] = useState<AdminVerificationLabel>('NONE');
  const [loaded, setLoaded] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 수정 대상은 목록 API 에서 찾는다 — 단건 조회 API 를 따로 만들지 않았다.
  useAsync(async () => {
    if (editingId === null || loaded) return null;
    const found = (await adminApi.users()).find((u) => u.id === editingId);
    if (found) {
      setLoginId(found.loginId);
      setNickname(found.nickname);
      setRole(found.role);
      setVerification(found.verificationStatus);
    }
    setLoaded(true);
    return found ?? null;
  }, [editingId, loaded]);

  async function save() {
    setBusy(true);
    setError(null);
    try {
      const body = {
        loginId: loginId.trim(),
        password: password.trim() || undefined,
        nickname: nickname.trim(),
        role,
        // 예술가가 아니면 인증 상태를 보내지 않는다 — 서버가 거절한다.
        verificationStatus: role === 'ARTIST' && verification !== 'NONE' ? verification : null,
      };
      if (editingId !== null) await adminApi.updateUser(editingId, body);
      else await adminApi.createUser(body);
      router.back();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '저장에 실패했어요.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen
      header={<ScreenHeader title={editingId !== null ? '회원 수정' : '회원 생성'} />}
      contentContainerStyle={{ gap: Spacing.lg, paddingHorizontal: Spacing.xl, paddingBottom: Spacing.huge }}>
      {error ? <AppText variant="caption" tint={theme.danger}>{error}</AppText> : null}

      <TextField
        label="로그인 아이디"
        value={loginId}
        onChangeText={setLoginId}
        placeholder="3~30자"
      />
      {editingId !== null ? (
        <AppText variant="caption" color="textMuted">아이디는 변경할 수 없어요.</AppText>
      ) : null}

      <TextField
        label={editingId !== null ? '비밀번호 (비우면 유지)' : '비밀번호'}
        value={password}
        onChangeText={setPassword}
        placeholder="4자 이상"
      />

      <TextField label="닉네임" value={nickname} onChangeText={setNickname} placeholder="20자 이하" />

      <ChipGroup
        label="회원 유형"
        options={ROLE_OPTIONS}
        selected={[role]}
        onToggle={(v) => setRole(v as Role)}
      />

      {role === 'ARTIST' ? (
        <ChipGroup
          label="예술가 인증 상태"
          options={VERIFICATION_OPTIONS}
          selected={[verification]}
          onToggle={(v) => setVerification(v as AdminVerificationLabel)}
        />
      ) : null}

      <View style={{ marginTop: Spacing.md }}>
        <Button label="저장" fullWidth loading={busy} onPress={save} />
      </View>
    </Screen>
  );
}
