import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, router } from 'expo-router';
import { ActivityIndicator, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { activityApi, spaceApi } from '@/lib/api';
import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { formatCurrency, formatDate, formatTimeRange } from '@/lib/format';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

const MATCHING_FEE = 5000;

export default function PaymentIndex() {
  const theme = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const activityId = Number(id);

  const { data: activity, loading } = useAsync(() => activityApi.detail(activityId), [activityId]);
  const { data: hostingRequest } = useAsync(async () => {
    if (!activity?.mine) return null;
    try {
      return await activityApi.hostingRequestStatus(activityId);
    } catch {
      return null;
    }
  }, [activityId, activity?.mine]);

  const { data: space } = useAsync(async () => {
    if (!hostingRequest?.spaceId) return null;
    return await spaceApi.detail(hostingRequest.spaceId);
  }, [hostingRequest?.spaceId]);

  if (loading || !activity) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
        <ScreenHeader title="공간 승인 및 결제하기" />
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
          <ActivityIndicator size="large" color={theme.primary} />
        </View>
      </SafeAreaView>
    );
  }

  const spaceFee = space?.hourlyFee ?? 0;
  const total = spaceFee + MATCHING_FEE;

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="공간 승인 및 결제하기" />
      <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.xl, paddingBottom: Spacing.huge }} showsVerticalScrollIndicator={false}>
        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h1">공간이 승인되었어요</AppText>
          <AppText variant="body" color="textSecondary">결제를 완료하면 프로그램이 공개됩니다.</AppText>
        </View>

        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">활동 정보</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
            <Row label="활동명" value={activity.title} />
            <Row label="날짜" value={formatDate(activity.date)} />
            <Row label="시간" value={formatTimeRange(activity.startTime, activity.endTime)} />
            <Row label="모집 인원" value={`${activity.capacity}명`} last />
          </Card>
        </View>

        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">공간 정보</AppText>
          {space ? (
            <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
              <Row label="공간명" value={space.name} />
              <Row label="지역" value={space.region} />
              {space.address && <Row label="주소" value={space.address} />}
              <Row label="공간 이용료" value={formatCurrency(space.hourlyFee)} tint={theme.primary} last />
            </Card>
          ) : (
            <Card tone="muted" padding="lg" radius="lg">
              <ActivityIndicator color={theme.primary} />
            </Card>
          )}
        </View>

        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">결제 금액</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
            <Row label="공간 이용료" value={formatCurrency(spaceFee)} />
            <Row label="아트민 매칭 이용료" value={formatCurrency(MATCHING_FEE)} />
            <View style={{ height: 1, backgroundColor: theme.border }} />
            <Row label="총 결제 금액" value={formatCurrency(total)} tint={theme.primary} last />
          </Card>
        </View>
      </ScrollView>

      <View style={{ padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        <Button label="결제하기" fullWidth size="lg" onPress={() => router.push(`/payment/${activityId}/receipt`)} />
      </View>
    </SafeAreaView>
  );
}

function Row({ label, value, last, tint }: { label: string; value: string; last?: boolean; tint?: string }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.xs, borderBottomWidth: last ? 0 : 1, borderBottomColor: theme.border, paddingBottom: last ? 0 : Spacing.md }}>
      <AppText variant="body" color="textMuted">{label}</AppText>
      <AppText variant="title" tint={tint} style={{ flex: 1, textAlign: 'right' }} numberOfLines={1}>{value}</AppText>
    </View>
  );
}
