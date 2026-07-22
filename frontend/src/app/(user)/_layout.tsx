import { Tabs } from 'expo-router';

import { TabBar, USER_TABS } from '@/components/nav';

export default function UserLayout() {
  return (
    <Tabs screenOptions={{ headerShown: false }} tabBar={(props) => <TabBar {...props} items={USER_TABS} />}>
      <Tabs.Screen name="index" />
      <Tabs.Screen name="activities" />
      <Tabs.Screen name="create" />
      <Tabs.Screen name="my-activities" />
    </Tabs>
  );
}
