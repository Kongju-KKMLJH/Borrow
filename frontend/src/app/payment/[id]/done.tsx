import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, router } from 'expo-router';
import { View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { activityApi } from '@/lib/api';
import { AppText, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { formatCurrency } from '@/lib/format';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

const MATCHING_FEE = 5000;

export default function PaymentDone() {
  const theme = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const activityId = Number(id);

  const { data: activity } = useAsync(() => activityApi.detail(activityId), [activityId]);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top', 'bottom']}>
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', padding: Spacing.xxxl, gap: Spacing.lg }}>
        <View style={{ width: 88, height: 88, borderRadius: 44, backgroundColor: theme.secondarySoft, alignItems: 'center', justifyContent: 'center' }}>
          <Ionicons name="checkmark" size={44} color={theme.secondary} />
        </View>
        <View style={{ alignItems: 'center', gap: Spacing.sm }}>
          <AppText variant="h1" center>결제 완료!</AppText>
          <AppText variant="body" color="textSecondary" center>프로그램이 공개되었습니다!</AppText>
        </View>

        {activity && (
          <Card tone="flat" padding="lg" radius="lg" style={{ width: '100%', gap: Spacing.md }}>
            <Row label="활동명" value={activity.title} />
            <Row label="참가비" value={formatCurrency(activity.entryFee)} />
            <Row label="매칭 이용료" value={formatCurrency(MATCHING_FEE)} last />
          </Card>
        )}
      </View>

      <View style={{ padding: Spacing.xl, gap: Spacing.sm }}>
        <Button label="프로그램 보기" fullWidth size="lg" onPress={() => router.push(`/activity/${activityId}`)} />
        <Button label="더 둘러보기" variant="outline" fullWidth onPress={() => router.replace('/(user)')} />
      </View>
    </SafeAreaView>
  );
}

function Row({ label, value, last }: { label: string; value: string; last?: boolean }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.xs, borderBottomWidth: last ? 0 : 1, borderBottomColor: theme.border, paddingBottom: last ? 0 : Spacing.md }}>
      <AppText variant="body" color="textMuted">{label}</AppText>
      <AppText variant="title" style={{ flex: 1, textAlign: 'right' }} numberOfLines={1}>{value}</AppText>
    </View>
  );
}
