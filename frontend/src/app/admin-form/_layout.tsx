import { Redirect, Stack } from 'expo-router';

import { useAuth } from '@/lib/auth';

/**
 * 관리자 데이터 생성·수정 폼 (기능명세 7.1.2 · 7.2.2 · 7.3.2).
 * 탭 밖의 스택 화면이라 (admin) 레이아웃의 가드가 닿지 않는다 — 여기서 다시 막는다.
 */
export default function AdminFormLayout() {
  const { user, loading } = useAuth();

  if (loading) return null;
  if (!user) return <Redirect href="/(auth)/login" />;
  if (user.role !== 'ADMIN') return <Redirect href="/(user)" />;

  return <Stack screenOptions={{ headerShown: false }} />;
}
