import { useLocalSearchParams, router } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { activityApi, spaceApi } from '@/lib/api';
import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { formatCurrency } from '@/lib/format';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

export default function PaymentReceipt() {
  const theme = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const activityId = Number(id);

  const { data: activity } = useAsync(() => activityApi.detail(activityId), [activityId]);
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

  const [paying, setPaying] = useState(false);

  const pay = async () => {
    setPaying(true);
    try {
      await activityApi.confirmPayment(activityId);
      router.push(`/payment/${activityId}/done`);
    } finally {
      setPaying(false);
    }
  };

  if (!activity || !space) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
        <ScreenHeader title="공간 승인 및 결제하기" />
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
          <ActivityIndicator size="large" color={theme.primary} />
        </View>
      </SafeAreaView>
    );
  }

  const price = hostingRequest?.price;
  const expectedRevenue = price?.expectedParticipantRevenue ?? 0;
  const spaceRentalFee = price?.spaceRentalFee ?? 0;
  const matchingFee = price?.platformMatchingFee ?? 0;
  const netRevenue = price?.expectedOperatingProfit ?? 0;

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="공간 승인 및 결제하기" />
      <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.xl, paddingBottom: Spacing.huge }} showsVerticalScrollIndicator={false}>
        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">공간 정보</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
            <Row label="공간명" value={space.name} />
            {space.address && <Row label="주소" value={space.address} />}
            <Row label="시간당 이용료" value={formatCurrency(space.hourlyFee)} tint={theme.primary} last />
          </Card>
        </View>

        <View style={{ backgroundColor: theme.secondarySoft, borderRadius: 16, padding: Spacing.lg, gap: Spacing.sm, flexDirection: 'row', alignItems: 'center' }}>
          <View style={{ width: 32, height: 32, borderRadius: 16, backgroundColor: theme.secondary, alignItems: 'center', justifyContent: 'center' }}>
            <AppText variant="small" tint={theme.textInverse}>✓</AppText>
          </View>
          <AppText variant="title" tint={theme.secondaryPressed}>공간 파트너가 개최 요청을 승인했습니다</AppText>
        </View>

        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">예상 운영 정산</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
            <Row label="예상 참가비 수익" value={formatCurrency(expectedRevenue)} />
            <Row label="공간 이용료" value={`-${formatCurrency(spaceRentalFee)}`} tint={theme.danger} />
            <Row label="아트민 매칭 이용료" value={`-${formatCurrency(matchingFee)}`} tint={theme.danger} />
            <View style={{ height: 1, backgroundColor: theme.border }} />
            <Row label="예상 운영 수익" value={formatCurrency(netRevenue)} tint={theme.primary} last />
          </Card>
          <AppText variant="caption" color="textMuted">예상 수익은 모집 정원 기준이며, 실제 수익은 참여 인원에 따라 달라집니다.</AppText>
        </View>
      </ScrollView>

      <View style={{ padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        <Button label={paying ? '' : '결제 및 프로그램 공개'} loading={paying} fullWidth size="lg" onPress={pay} />
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
