import { Image } from 'expo-image';
import { router } from 'expo-router';
import { View } from 'react-native';

import { hostApi, imageUri, spaceApi } from '@/lib/api';
import { ImagePlaceholder } from '@/components/placeholder';
import { BrandHeader, ModeSwitch } from '@/components/nav';
import { RequestCard } from '@/components/request-card';
import { AppText, Button, Card, Screen, SectionHeader } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { formatDateTime } from '@/lib/format';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

export default function ProviderHome() {
  const theme = useTheme();
  const { data: home } = useAsync(() => hostApi.home(), [], { refetchOnFocus: true });
  const { data: spaces } = useAsync(() => spaceApi.list(), [], { refetchOnFocus: true });
  const space = spaces?.[0];

  return (
    <Screen
      header={<BrandHeader right={<ModeSwitch current="provider" />} subtitle="사장님, 안녕하세요 👋" />}
      contentContainerStyle={{ gap: Spacing.xxl, paddingBottom: Spacing.huge }}>
      {/* 요약 히어로 (화이트 + 코랄 아웃라인) */}
      <View style={{ paddingHorizontal: Spacing.xl }}>
        <View style={{ backgroundColor: theme.surface, borderRadius: Radius.xl, borderWidth: 1.5, borderColor: theme.primary + '40', padding: Spacing.xl, gap: Spacing.lg }}>
          <AppText variant="label" color="text">이번 주 공간 현황</AppText>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Stat n={home?.pendingRequestCount ?? 0} label="승인 대기" />
            <Divider />
            <Stat n={home?.confirmedScheduleCount ?? 0} label="확정 일정" />
            <Divider />
            <Stat n={home?.spaceCount ?? 0} label="등록 공간" />
          </View>
        </View>
      </View>

      {/* 내 공간 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        <AppText variant="h3">내 공간</AppText>
        {space ? (
          <Card tone="flat" padding="md" radius="lg" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
            {space.imageUrls?.[0] ? (
              <Image source={{ uri: imageUri(space.imageUrls[0]) }} style={{ width: 64, height: 64, borderRadius: Radius.md, backgroundColor: theme.surfaceMuted }} contentFit="cover" transition={200} />
            ) : (
              <ImagePlaceholder height={64} radius="md" style={{ width: 64 }} iconSize={24} />
            )}
            <View style={{ flex: 1, gap: 3 }}>
              <AppText variant="title">{space.name}</AppText>
              <AppText variant="caption" color="textMuted">{space.region} · 최대 {space.capacity}명</AppText>
            </View>
            <Button label="수정" variant="outline" size="sm" onPress={() => router.push('/(provider)/space')} />
          </Card>
        ) : (
          <Card tone="muted" padding="lg" radius="lg" shadow="none" style={{ gap: Spacing.sm, alignItems: 'center' }}>
            <AppText variant="body" color="textSecondary">등록된 공간이 없어요</AppText>
            <Button label="공간 등록하기" onPress={() => router.push('/(provider)/space')} />
          </Card>
        )}
      </View>

      {/* 새로운 개최 요청 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        <SectionHeader title="새로운 개최 요청" actionLabel="전체 보기" onAction={() => router.push('/(provider)/requests')} style={{ marginBottom: 0 }} />
        {(home?.recentPendingRequests ?? []).map((r) => (
          <RequestCard key={r.id} request={r} onPress={() => router.push(`/request/${r.id}`)} onApprove={() => router.push(`/request/${r.id}`)} onReject={() => router.push(`/request/${r.id}`)} />
        ))}
        {home && home.recentPendingRequests.length === 0 && (
          <AppText variant="caption" color="textMuted">새로운 개최 요청이 없어요</AppText>
        )}
      </View>

      {/* 예정된 활동 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        <AppText variant="h3">예정된 활동</AppText>
        <Card tone="flat" padding="lg" radius="lg" style={{ gap: 0 }}>
          {(home?.upcomingSchedules ?? []).map((s, i, arr) => (
            <Upcoming key={s.requestId} title={s.title} date={formatDateTime(s.date, s.startTime, s.endTime)} people={`${s.capacity}명`} last={i === arr.length - 1} />
          ))}
          {home && home.upcomingSchedules.length === 0 && (
            <AppText variant="caption" color="textMuted">예정된 활동이 없어요</AppText>
          )}
        </Card>
      </View>
    </Screen>
  );
}

function Stat({ n, label }: { n: number; label: string }) {
  return (
    <View style={{ flex: 1, alignItems: 'center', gap: 4 }}>
      <AppText variant="h1" color="text">{n}</AppText>
      <AppText variant="caption" color="textMuted">{label}</AppText>
    </View>
  );
}
function Divider() {
  const theme = useTheme();
  return <View style={{ width: 1, height: 34, backgroundColor: theme.border }} />;
}
function Upcoming({ title, date, people, last }: { title: string; date: string; people: string; last?: boolean }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: Spacing.md, borderBottomWidth: last ? 0 : 1, borderBottomColor: theme.border }}>
      <View style={{ flex: 1, gap: 3 }}>
        <AppText variant="title">{title}</AppText>
        <AppText variant="caption" color="textMuted">{date}</AppText>
      </View>
      <AppText variant="caption" tint={theme.secondary}>{people}</AppText>
    </View>
  );
}
