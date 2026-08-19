import { Redirect, Tabs } from 'expo-router';

import { ADMIN_TABS, TabBar } from '@/components/nav';
import { useAuth } from '@/lib/auth';

/**
 * 관리자 콘솔 (기능명세 7). 진입 자체를 역할로 막는다 —
 * 백엔드가 /api/admin/** 을 ADMIN 으로 잠그고 있지만, 가드가 없으면 다른 역할이
 * 라우트를 눌러 들어와 403 만 가득한 빈 화면을 보게 된다.
 */
export default function AdminLayout() {
  const { user, loading } = useAuth();

  if (loading) return null;
  if (!user) return <Redirect href="/(auth)/login" />;
  if (user.role !== 'ADMIN') return <Redirect href="/(user)" />;

  return (
    <Tabs screenOptions={{ headerShown: false }} tabBar={(props) => <TabBar {...props} items={ADMIN_TABS} />}>
      <Tabs.Screen name="index" />
      <Tabs.Screen name="members" />
      <Tabs.Screen name="activities" />
      <Tabs.Screen name="spaces" />
    </Tabs>
  );
}
