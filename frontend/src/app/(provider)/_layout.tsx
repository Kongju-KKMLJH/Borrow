import { Redirect, Tabs } from 'expo-router';

import { PROVIDER_TABS, TabBar } from '@/components/nav';
import { useAuth } from '@/lib/auth';

/**
 * 공간 제공자 모드. 진입 자체를 역할로 막는다 —
 * 백엔드가 /api/host/** 을 HOST 로 잠그고 있지만, 가드가 없으면 다른 역할이
 * 라우트를 눌러 들어와 403 만 가득한 빈 화면을 보게 된다.
 */
export default function ProviderLayout() {
  const { user, loading } = useAuth();

  if (loading) return null;
  if (!user) return <Redirect href="/(auth)/login" />;
  if (user.role !== 'HOST') return <Redirect href="/(user)" />;

  return (
    <Tabs screenOptions={{ headerShown: false }} tabBar={(props) => <TabBar {...props} items={PROVIDER_TABS} />}>
      <Tabs.Screen name="index" />
      <Tabs.Screen name="requests" />
      <Tabs.Screen name="space" />
    </Tabs>
  );
}
