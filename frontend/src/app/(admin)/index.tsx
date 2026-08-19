import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { View } from 'react-native';

import { BrandHeader } from '@/components/nav';
import { AppText, Button, Card, Screen } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';
import { adminApi } from '@/lib/api';
import { useAuth } from '@/lib/auth';

/** 관리자 홈 — 회원·프로그램·공간 관리로 들어가는 허브 (유저플로우 "관리자 콘솔") */
const MENUS = [
  { href: '/(admin)/members', label: '회원 관리', desc: '전체 회원 · 강제 탈퇴 · 예술가 인증 승인', icon: 'people-outline' },
  { href: '/(admin)/activities', label: '프로그램 관리', desc: '전체 프로그램 · 강제 삭제', icon: 'documents-outline' },
  { href: '/(admin)/spaces', label: '공간 관리', desc: '전체 공간 · 강제 삭제', icon: 'storefront-outline' },
] as const;

export default function AdminHome() {
  const theme = useTheme();
  const { logout } = useAuth();

  const { data: users } = useAsync(() => adminApi.users(), [], { refetchOnFocus: true });
  const { data: activities } = useAsync(() => adminApi.activities(), [], { refetchOnFocus: true });
  const { data: spaces } = useAsync(() => adminApi.spaces(), [], { refetchOnFocus: true });
  const { data: pending } = useAsync(() => adminApi.verifications('PENDING'), [], { refetchOnFocus: true });

  return (
    <Screen
      header={
        <BrandHeader
          subtitle="관리자 콘솔"
          right={<Button label="로그아웃" variant="ghost" size="sm" onPress={() => logout()} />}
        />
      }
      contentContainerStyle={{ gap: Spacing.xxl, paddingBottom: Spacing.huge }}>
      {/* 운영 현황 요약 */}
      <View style={{ paddingHorizontal: Spacing.xl }}>
        <View
          style={{
            backgroundColor: theme.surface,
            borderRadius: Radius.xl,
            borderWidth: 1.5,
            borderColor: theme.primary + '40',
            padding: Spacing.xl,
            gap: Spacing.lg,
          }}>
          <AppText variant="label" color="text">시범 운영 현황</AppText>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Metric n={users?.length ?? 0} label="회원" />
            <Bar />
            <Metric n={activities?.length ?? 0} label="프로그램" />
            <Bar />
            <Metric n={spaces?.length ?? 0} label="공간" />
          </View>
        </View>
      </View>

      {/* 심사 대기 알림 — 관리자가 가장 먼저 해야 할 일 */}
      {pending && pending.length > 0 ? (
        <View style={{ paddingHorizontal: Spacing.xl }}>
          <Card tone="muted" padding="lg" radius="lg" shadow="none" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
            <Ionicons name="ribbon-outline" size={22} color={theme.primary} />
            <View style={{ flex: 1, gap: 2 }}>
              <AppText variant="title">예술가 인증 심사 {pending.length}건</AppText>
              <AppText variant="caption" color="textMuted">승인해야 인증 배지가 붙습니다</AppText>
            </View>
            <Button label="확인" variant="outline" size="sm" onPress={() => router.push('/(admin)/members')} />
          </Card>
        </View>
      ) : null}

      {/* 관리 메뉴 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        <AppText variant="h3">관리 메뉴</AppText>
        {MENUS.map((m) => (
          <Card
            key={m.href}
            tone="flat"
            padding="lg"
            radius="lg"
            onPress={() => router.push(m.href)}
            style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
            <View
              style={{
                width: 44,
                height: 44,
                borderRadius: Radius.md,
                backgroundColor: theme.primarySoft,
                alignItems: 'center',
                justifyContent: 'center',
              }}>
              <Ionicons name={m.icon} size={22} color={theme.primary} />
            </View>
            <View style={{ flex: 1, gap: 3 }}>
              <AppText variant="title">{m.label}</AppText>
              <AppText variant="caption" color="textMuted">{m.desc}</AppText>
            </View>
            <Ionicons name="chevron-forward" size={18} color={theme.textMuted} />
          </Card>
        ))}
      </View>
    </Screen>
  );
}

function Metric({ n, label }: { n: number; label: string }) {
  return (
    <View style={{ flex: 1, alignItems: 'center', gap: 2 }}>
      <AppText variant="h2">{n}</AppText>
      <AppText variant="caption" color="textMuted">{label}</AppText>
    </View>
  );
}

function Bar() {
  const theme = useTheme();
  return <View style={{ width: 1, height: 28, backgroundColor: theme.border }} />;
}
