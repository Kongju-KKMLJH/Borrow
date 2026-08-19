import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Pressable, ScrollView, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAuth } from '@/lib/auth';
import type { Role } from '@/lib/api/types';
import { AppText, Button } from '@/components/ui';
import { FontFamily, Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const ROLES: { value: Role; label: string; desc: string; icon: keyof typeof Ionicons.glyphMap }[] = [
  { value: 'MEMBER', label: '일반 회원', desc: '취미 모임 개설 · 참여', icon: 'people-outline' },
  { value: 'HOST', label: '공간 제공자', desc: '유휴 공간 등록 · 승인', icon: 'storefront-outline' },
  { value: 'ARTIST', label: '예술가', desc: '전문 클래스 개설 · 참여', icon: 'ribbon-outline' },
];

export default function LoginScreen() {
  const { login, signup } = useAuth();
  const theme = useTheme();
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [role, setRole] = useState<Role>('MEMBER');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit() {
    if (!loginId.trim() || !password.trim()) {
      setError('아이디와 비밀번호를 입력해주세요.');
      return;
    }
    if (mode === 'signup' && !nickname.trim()) {
      setError('닉네임을 입력해주세요.');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      if (mode === 'login') {
        await login(loginId.trim(), password);
      } else {
        await signup({ loginId: loginId.trim(), password, nickname: nickname.trim(), role });
      }
      // 역할 분기는 진입 화면 한 곳(app/index.tsx)에만 둔다 — login() 이 역할을 돌려주지 않고,
      // 분기를 두 벌로 만들면 관리자 라우팅이 한쪽에서만 갱신된다.
      router.replace('/');
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '오류가 발생했어요.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top', 'bottom']}>
      <ScrollView contentContainerStyle={{ paddingHorizontal: Spacing.xl, paddingVertical: Spacing.huge, gap: Spacing.xxxl, maxWidth: 480, alignSelf: 'center', width: '100%' }}>
        <View style={{ alignItems: 'center', gap: Spacing.sm, paddingTop: Spacing.huge }}>
          <AppText variant="display" tint={theme.primary} style={{ fontSize: 36 }}>아트민</AppText>
          <AppText variant="body" color="textSecondary" center>천안의 유휴공간에서 함께하는 취미</AppText>
        </View>

        <View style={{ flexDirection: 'row', backgroundColor: theme.surfaceMuted, borderRadius: Radius.full, padding: 4 }}>
          {(['login', 'signup'] as const).map((m) => (
            <Pressable
              key={m}
              onPress={() => { setMode(m); setError(null); }}
              style={{ flex: 1, paddingVertical: Spacing.sm + 2, borderRadius: Radius.full, alignItems: 'center', backgroundColor: mode === m ? theme.primary : 'transparent' }}>
              <AppText variant="label" tint={mode === m ? theme.textInverse : theme.textSecondary}>
                {m === 'login' ? '로그인' : '회원가입'}
              </AppText>
            </Pressable>
          ))}
        </View>

        <View style={{ gap: Spacing.lg }}>
          <InputField label="아이디" value={loginId} onChangeText={setLoginId} placeholder="로그인 아이디" autoCapitalize="none" />
          <InputField label="비밀번호" value={password} onChangeText={setPassword} placeholder="비밀번호" secureTextEntry />

          {mode === 'signup' && (
            <View style={{ gap: Spacing.lg }}>
              <InputField label="닉네임" value={nickname} onChangeText={setNickname} placeholder="표시될 이름" />
              <View style={{ gap: Spacing.sm }}>
                <AppText variant="label" color="textSecondary">회원 유형</AppText>
                <View style={{ gap: Spacing.sm }}>
                  {ROLES.map((r) => {
                    const selected = role === r.value;
                    return (
                      <Pressable
                        key={r.value}
                        onPress={() => setRole(r.value)}
                        style={{
                          flexDirection: 'row', alignItems: 'center', gap: Spacing.md,
                          paddingHorizontal: Spacing.lg, paddingVertical: Spacing.md,
                          borderRadius: Radius.md,
                          backgroundColor: selected ? theme.primarySoft : theme.surfaceMuted,
                          borderWidth: 1.5, borderColor: selected ? theme.primary : theme.border,
                        }}>
                        <Ionicons name={r.icon} size={22} color={selected ? theme.primary : theme.textSecondary} />
                        <View style={{ flex: 1 }}>
                          <AppText variant="title" tint={selected ? theme.primary : theme.text}>{r.label}</AppText>
                          <AppText variant="caption" color="textMuted">{r.desc}</AppText>
                        </View>
                        {selected && <Ionicons name="checkmark-circle" size={20} color={theme.primary} />}
                      </Pressable>
                    );
                  })}
                </View>
              </View>
            </View>
          )}

          {error && (
            <View style={{ backgroundColor: theme.dangerSoft, borderRadius: Radius.md, paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm }}>
              <AppText variant="caption" tint={theme.danger}>{error}</AppText>
            </View>
          )}

          <Button label={mode === 'login' ? '로그인' : '가입하기'} onPress={handleSubmit} loading={loading} fullWidth size="lg" />
        </View>

        {mode === 'login' && (
          <AppText variant="caption" color="textMuted" center>
            계정이 없으신가요?{' '}
            <AppText variant="caption" tint={theme.primary} onPress={() => { setMode('signup'); setError(null); }}>
              회원가입
            </AppText>
          </AppText>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

function InputField({
  label, value, onChangeText, placeholder, secureTextEntry, autoCapitalize,
}: {
  label: string; value: string; onChangeText: (t: string) => void; placeholder?: string; secureTextEntry?: boolean; autoCapitalize?: 'none' | 'sentences' | 'words';
}) {
  const theme = useTheme();
  return (
    <View style={{ gap: Spacing.sm }}>
      <AppText variant="label" color="textSecondary">{label}</AppText>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={theme.textMuted}
        secureTextEntry={secureTextEntry}
        autoCapitalize={autoCapitalize}
        style={{
          backgroundColor: theme.surfaceMuted,
          borderRadius: Radius.md,
          paddingHorizontal: 14,
          paddingVertical: 13,
          fontFamily: FontFamily.regular,
          fontSize: 15,
          color: theme.text,
        }}
      />
    </View>
  );
}
