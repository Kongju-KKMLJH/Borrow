import { Tabs } from 'expo-router';

import { PROVIDER_TABS, TabBar } from '@/components/nav';

export default function ProviderLayout() {
  return (
    <Tabs screenOptions={{ headerShown: false }} tabBar={(props) => <TabBar {...props} items={PROVIDER_TABS} />}>
      <Tabs.Screen name="index" />
      <Tabs.Screen name="requests" />
      <Tabs.Screen name="space" />
    </Tabs>
  );
}
