import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { Alert, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppText, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export default function SubscriptionDone() {
  const theme = useTheme();

  const nextMonth = new Date();
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  const dateStr = `${nextMonth.getFullYear()}년 ${nextMonth.getMonth() + 1}월 ${String(nextMonth.getDate()).padStart(2, '0')}일`;

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top', 'bottom']}>
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', padding: Spacing.xxxl, gap: Spacing.lg }}>
        <View style={{ width: 88, height: 88, borderRadius: 44, backgroundColor: theme.secondarySoft, alignItems: 'center', justifyContent: 'center' }}>
          <Ionicons name="checkmark" size={44} color={theme.secondary} />
        </View>
        <View style={{ alignItems: 'center', gap: Spacing.sm }}>
          <AppText variant="h1" center>구독 완료!</AppText>
          <AppText variant="body" color="textSecondary" center>이제 ARTMIN PRO 혜택으로{'\n'}더 자유롭게 프로그램을 운영해보세요.</AppText>
        </View>

        <Card tone="flat" padding="lg" radius="lg" style={{ width: '100%', gap: Spacing.md }}>
          <Row label="다음 결제 예정일" value={dateStr} />
          <Row label="결제 주기" value="매월 자동 결제됩니다." last />
        </Card>
      </View>

      <View style={{ padding: Spacing.xl, gap: Spacing.sm }}>
        <Button label="프로그램 만들기" fullWidth size="lg" onPress={() => router.replace('/(user)/create')} />
        <Button label="구독 관리하기" variant="outline" fullWidth onPress={() => Alert.alert('준비 중', '구독 관리 기능은 곧 출시됩니다.')} />
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
