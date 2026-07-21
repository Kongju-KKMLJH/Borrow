import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, View } from 'react-native';

import { ActivityCard } from '@/components/activity-card';
import { ActivityStatusBadge } from '@/components/status-badge';
import { AppText, Button, Card, Screen } from '@/components/ui';
import { ImagePlaceholder } from '@/components/placeholder';
import { Spacing } from '@/constants/theme';
import { myCreated, myJoined } from '@/data/mock';
import type { Activity } from '@/data/types';
import { useTheme } from '@/hooks/use-theme';

export default function MyActivities() {
  const theme = useTheme();
  const [tab, setTab] = useState<'joined' | 'created'>('created');

  return (
    <Screen
      header={
        <View style={{ paddingHorizontal: Spacing.xl, paddingTop: Spacing.md, paddingBottom: Spacing.sm }}>
          <AppText variant="h2">내 활동</AppText>
        </View>
      }
      contentContainerStyle={{ gap: Spacing.lg, paddingBottom: Spacing.huge }}>
      {/* 탭 */}
      <View style={{ flexDirection: 'row', paddingHorizontal: Spacing.xl }}>
        <Tab label="참여한 활동" active={tab === 'joined'} onPress={() => setTab('joined')} />
        <Tab label="만든 활동" active={tab === 'created'} onPress={() => setTab('created')} />
      </View>

      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        {tab === 'joined'
          ? myJoined.map((a) => <ActivityCard key={a.id} activity={a} onPress={() => router.push(`/activity/${a.id}`)} />)
          : myCreated.map((a) => (a.status === 'rejected' ? <RejectedCard key={a.id} activity={a} /> : <CreatedRow key={a.id} activity={a} onPress={() => router.push(`/activity/${a.id}`)} />))}
      </View>
    </Screen>
  );
}

function Tab({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  const theme = useTheme();
  return (
    <Pressable onPress={onPress} style={{ flex: 1, alignItems: 'center', gap: 10 }}>
      <AppText variant="title" color={active ? 'text' : 'textMuted'}>{label}</AppText>
      <View style={{ height: 2, alignSelf: 'stretch', backgroundColor: active ? theme.text : 'transparent' }} />
    </Pressable>
  );
}

function CreatedRow({ activity, onPress }: { activity: Activity; onPress?: () => void }) {
  const theme = useTheme();
  return (
    <Card onPress={onPress} tone="flat" padding="md" radius="lg" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
      <ImagePlaceholder field={activity.field} height={56} radius="md" style={{ width: 56 }} iconSize={22} />
      <View style={{ flex: 1, gap: 6 }}>
        <ActivityStatusBadge status={activity.status} />
        <AppText variant="title" numberOfLines={1}>{activity.title}</AppText>
        <AppText variant="caption" color="textMuted">{activity.space?.name} · 모집 {activity.joined}/{activity.capacity}명</AppText>
      </View>
      <Ionicons name="chevron-forward" size={18} color={theme.textMuted} />
    </Card>
  );
}

function RejectedCard({ activity }: { activity: Activity }) {
  const theme = useTheme();
  return (
    <Card padding="lg" radius="lg" style={{ gap: Spacing.md, backgroundColor: theme.dangerSoft, borderWidth: 1, borderColor: theme.danger + '40' }} shadow="none">
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
        <ActivityStatusBadge status="rejected" />
        <AppText variant="title" style={{ flex: 1 }} numberOfLines={1}>{activity.title}</AppText>
      </View>
      <AppText variant="caption" tint={theme.danger}>거절 사유: 요청 시간에 다른 예약이 있어 공간 제공이 어렵습니다.</AppText>
      <Button label="대체 공간 추천받기" variant="outline" fullWidth onPress={() => router.push('/(user)/create')} />
    </Card>
  );
}
