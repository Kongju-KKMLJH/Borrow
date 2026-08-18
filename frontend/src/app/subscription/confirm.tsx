import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Card } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const STEPS = ['구독 소개', '결제 확인', '완료'];

export default function SubscriptionConfirm() {
  const theme = useTheme();

  const nextMonth = new Date();
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  const dateStr = `${nextMonth.getFullYear()}년 ${nextMonth.getMonth() + 1}월 ${String(nextMonth.getDate()).padStart(2, '0')}일`;

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="구독 결제" />
      <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.xl, paddingBottom: Spacing.huge }} showsVerticalScrollIndicator={false}>
        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: Spacing.sm }}>
          {STEPS.map((s, i) => (
            <View key={s} style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
              <View style={{ width: 28, height: 28, borderRadius: 14, backgroundColor: i <= 1 ? theme.primary : theme.surfaceMuted, alignItems: 'center', justifyContent: 'center' }}>
                <AppText variant="tiny" tint={i <= 1 ? theme.textInverse : theme.textMuted}>{i + 1}</AppText>
              </View>
              <AppText variant="small" color={i === 1 ? 'text' : 'textMuted'}>{s}</AppText>
              {i < STEPS.length - 1 && <View style={{ width: 24, height: 2, backgroundColor: i < 1 ? theme.primary : theme.surfaceDeep }} />}
            </View>
          ))}
        </View>

        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">플랜</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md, borderWidth: 1.5, borderColor: theme.primary + '40', backgroundColor: theme.primarySoft }}>
            <AppText variant="h2" tint={theme.primary}>ARTMIN PRO</AppText>
            <AppText variant="title">월 구독료 10,000원</AppText>
          </Card>
        </View>

        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">결제 수단</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
            <View style={{ width: 40, height: 40, borderRadius: 20, backgroundColor: theme.surfaceMuted, alignItems: 'center', justifyContent: 'center' }}>
              <Ionicons name="card" size={20} color={theme.text} />
            </View>
            <AppText variant="title">카드 결제</AppText>
          </Card>
        </View>

        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">결제 일정</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
            <Row label="다음 결제 예정일" value={dateStr} />
            <Row label="결제 주기" value="매월 자동 결제" last />
          </Card>
        </View>

        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: Spacing.sm }}>
          <AppText variant="title" color="textSecondary">총 결제 금액</AppText>
          <AppText variant="h1" tint={theme.primary}>10,000원<span style={{ fontSize: 14 }}> /월</span></AppText>
        </View>
      </ScrollView>

      <View style={{ padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        <Button label="결제하기" fullWidth size="lg" onPress={() => router.push('/subscription/done')} />
      </View>
    </SafeAreaView>
  );
}

function Row({ label, value, last }: { label: string; value: string; last?: boolean }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.xs, borderBottomWidth: last ? 0 : 1, borderBottomColor: theme.border, paddingBottom: last ? 0 : Spacing.md }}>
      <AppText variant="body" color="textMuted">{label}</AppText>
      <AppText variant="title">{value}</AppText>
    </View>
  );
}
