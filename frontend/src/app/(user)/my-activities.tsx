import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, View } from 'react-native';

import { meApi } from '@/lib/api';
import { ActivityCard } from '@/components/activity-card';
import { ActivityStatusBadge } from '@/components/status-badge';
import { AppText, Button, Card, Screen } from '@/components/ui';
import { ImagePlaceholder } from '@/components/placeholder';
import { Spacing } from '@/constants/theme';
import type { ActivitySummaryResponse } from '@/lib/api/types';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

export default function MyActivities() {
  const [tab, setTab] = useState<'joined' | 'created'>('created');
  const { data: participations } = useAsync(() => meApi.myParticipations(), [], { refetchOnFocus: true });
  const { data: created } = useAsync(() => meApi.myActivities(), [], { refetchOnFocus: true });

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
        {tab === 'joined' ? (
          (participations ?? []).length === 0 ? (
            <EmptyState
              icon="ticket-outline"
              title="참여한 활동이 없어요"
              body="관심 있는 활동에 참여하면 여기에 모여요."
              actionLabel="활동 둘러보기"
              onAction={() => router.push('/(user)/activities')}
            />
          ) : (
            (participations ?? []).map((p) => (
              <ActivityCard key={p.participationId} activity={p.activity} onPress={() => router.push(`/activity/${p.activity.id}`)} />
            ))
          )
        ) : (created ?? []).length === 0 ? (
          <EmptyState
            icon="add-circle-outline"
            title="만든 활동이 없어요"
            body="원하는 활동을 만들면 AI가 딱 맞는 공간을 추천해드려요."
            actionLabel="활동 만들기"
            onAction={() => router.push('/(user)/create')}
          />
        ) : (
          (created ?? []).map((a) =>
            a.status === 'REJECTED' ? (
              <RejectedCard key={a.id} activity={a} />
            ) : (
              <CreatedRow key={a.id} activity={a} onPress={() => router.push(`/activity/${a.id}`)} />
            ),
          )
        )}
      </View>
    </Screen>
  );
}

function EmptyState({
  icon,
  title,
  body,
  actionLabel,
  onAction,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  title: string;
  body: string;
  actionLabel: string;
  onAction: () => void;
}) {
  const theme = useTheme();
  return (
    <View style={{ alignItems: 'center', paddingVertical: Spacing.huge, gap: Spacing.md }}>
      <Ionicons name={icon} size={40} color={theme.textMuted} />
      <View style={{ alignItems: 'center', gap: 4 }}>
        <AppText variant="title" color="textSecondary">{title}</AppText>
        <AppText variant="caption" color="textMuted" style={{ textAlign: 'center' }}>{body}</AppText>
      </View>
      <Button label={actionLabel} variant="outline" onPress={onAction} />
    </View>
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

function CreatedRow({ activity, onPress }: { activity: ActivitySummaryResponse; onPress?: () => void }) {
  const theme = useTheme();
  return (
    <Card onPress={onPress} tone="flat" padding="md" radius="lg" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
      <ImagePlaceholder field={activity.field} height={56} radius="md" style={{ width: 56 }} iconSize={22} />
      <View style={{ flex: 1, gap: 6 }}>
        <ActivityStatusBadge status={activity.status} />
        <AppText variant="title" numberOfLines={1}>{activity.title}</AppText>
        <AppText variant="caption" color="textMuted">모집 {activity.currentHeadcount}/{activity.capacity}명</AppText>
      </View>
      <Ionicons name="chevron-forward" size={18} color={theme.textMuted} />
    </Card>
  );
}

function RejectedCard({ activity }: { activity: ActivitySummaryResponse }) {
  const theme = useTheme();
  return (
    <Card padding="lg" radius="lg" style={{ gap: Spacing.md, backgroundColor: theme.dangerSoft, borderWidth: 1, borderColor: theme.danger + '40' }} shadow="none">
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
        <ActivityStatusBadge status="REJECTED" />
        <AppText variant="title" style={{ flex: 1 }} numberOfLines={1}>{activity.title}</AppText>
      </View>
      <AppText variant="caption" tint={theme.danger}>공간 제공자가 개최 요청을 거절했어요. 다른 공간을 다시 추천받아보세요.</AppText>
      <Button label="대체 공간 추천받기" variant="outline" fullWidth onPress={() => router.push('/(user)/create')} />
    </Card>
  );
}
