import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { hostApi, spaceApi } from '@/lib/api';
import { ScreenHeader } from '@/components/nav';
import { RequestStatusBadge } from '@/components/status-badge';
import { AppText, Avatar, Badge, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { ActivityFieldLabel, FacilityTypeLabel, formatCurrency, formatDate, formatTimeRange } from '@/lib/format';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

export default function RequestDetail() {
  const theme = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const requestId = Number(id);
  const { data: req, loading } = useAsync(() => hostApi.requestDetail(requestId), [requestId]);
  const { data: space } = useAsync(() => (req ? spaceApi.detail(req.space.id) : Promise.resolve(null)), [req?.space.id]);

  if (loading || !req) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }}>
        <ScreenHeader title="개최 요청 상세" />
      </SafeAreaView>
    );
  }
  const { activity } = req;
  const requiredFacilities = activity.requirement?.requiredFacilities ?? [];
  const spaceFacilities = new Set(space?.facilities ?? []);

  const approve = async () => { await hostApi.approve(req.id); router.back(); };
  const reject = async () => { await hostApi.reject(req.id); router.back(); };

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="개최 요청 상세" />
      <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.xl, paddingBottom: Spacing.xxxl }} showsVerticalScrollIndicator={false}>
        {/* 활동 정보 */}
        <View style={{ gap: Spacing.md }}>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
            <Badge label={ActivityFieldLabel[activity.field]} tone="primary" />
            <View style={{ flex: 1 }} />
            <RequestStatusBadge status={req.status} />
          </View>
          <AppText variant="h1">{activity.title}</AppText>
          {activity.description ? <AppText variant="body" color="textSecondary">{activity.description}</AppText> : null}
          <Card tone="muted" padding="lg" radius="lg" shadow="none" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
            <Avatar name={activity.hostNickname ?? undefined} size={40} />
            <View style={{ flex: 1, gap: 3 }}>
              <AppText variant="title">{activity.hostNickname}</AppText>
              <AppText variant="caption" color="textMuted">진행자</AppText>
            </View>
          </Card>
        </View>

        <Section title="일정">
          <Row label="활동 날짜" value={formatDate(activity.date)} />
          <Row label="활동 시간" value={formatTimeRange(activity.startTime, activity.endTime)} last />
        </Section>

        <Section title="참여">
          <Row label="모집 인원" value={`${activity.capacity}명`} />
          <Row label="참가비" value={formatCurrency(activity.entryFee)} last />
        </Section>

        {/* 요청 시설 */}
        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">요청 시설</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
            {requiredFacilities.length === 0 && <AppText variant="body" color="textMuted">요청한 시설이 없어요</AppText>}
            {requiredFacilities.map((f) => {
              const ok = spaceFacilities.has(f);
              return (
                <View key={f} style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                  <Ionicons name={ok ? 'checkmark-circle' : 'close-circle'} size={18} color={ok ? theme.secondary : theme.textMuted} />
                  <AppText variant="body" color={ok ? 'text' : 'textMuted'}>{FacilityTypeLabel[f]}</AppText>
                </View>
              );
            })}
          </Card>
        </View>

        {/* 활동 특성 */}
        {activity.requirement && (activity.requirement.noisy || activity.requirement.messy) && (
          <View style={{ backgroundColor: theme.accentSoft, borderRadius: 16, padding: Spacing.lg, gap: Spacing.sm }}>
            <AppText variant="title" tint={theme.accentPressed}>확인해주세요</AppText>
            {activity.requirement.noisy && <AppText variant="caption" color="textSecondary">소음이 발생할 수 있는 활동이에요.</AppText>}
            {activity.requirement.messy && <AppText variant="caption" color="textSecondary">오염(물감 등)이 발생할 수 있는 활동이에요.</AppText>}
          </View>
        )}
      </ScrollView>

      {/* 승인/거절 */}
      {req.status === 'PENDING' && (
        <View style={{ flexDirection: 'row', gap: Spacing.sm, padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
          <Button label="개최 거절" variant="outline" onPress={reject} style={{ flex: 1 }} />
          <Button label="개최 승인" onPress={approve} style={{ flex: 1 }} />
        </View>
      )}
    </SafeAreaView>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={{ gap: Spacing.sm }}>
      <AppText variant="h3">{title}</AppText>
      <Card tone="flat" padding="lg" radius="lg" style={{ gap: 0 }}>{children}</Card>
    </View>
  );
}
function Row({ label, value, last }: { label: string; value: string; last?: boolean }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.md, borderBottomWidth: last ? 0 : 1, borderBottomColor: theme.border }}>
      <AppText variant="body" color="textMuted">{label}</AppText>
      <AppText variant="title">{value}</AppText>
    </View>
  );
}
