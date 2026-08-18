import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Card } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const BENEFITS = [
  { icon: 'pricetag-outline' as const, title: '매칭 이용료 할인', desc: '매칭 수수료 50% 할인' },
  { icon: 'trending-up' as const, title: '우선 노출', desc: '프로그램이 목록 상단에 노출됩니다' },
  { icon: 'wallet-outline' as const, title: '정산 관리 강화', desc: '수익 정산을 한 곳에서 관리하세요' },
  { icon: 'star-outline' as const, title: 'PRO 전용 혜택', desc: '월 1회 무료 공간 프로모션' },
];

export default function SubscriptionIntro() {
  const theme = useTheme();

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="아트민과 함께, 더 자유롭게" />
      <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.xxl, paddingBottom: Spacing.huge }} showsVerticalScrollIndicator={false}>
        <View style={{ alignItems: 'center', gap: Spacing.sm, paddingTop: Spacing.xl }}>
          <AppText variant="display" tint={theme.primary}>ARTMIN PRO</AppText>
          <AppText variant="body" color="textSecondary" center>프로그램 운영을 더 쉽고, 더 강력하게</AppText>
        </View>

        <View style={{ gap: Spacing.md }}>
          {BENEFITS.map((b) => (
            <View key={b.title} style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
              <View style={{ width: 44, height: 44, borderRadius: 22, backgroundColor: theme.primarySoft, alignItems: 'center', justifyContent: 'center' }}>
                <Ionicons name={b.icon} size={22} color={theme.primary} />
              </View>
              <View style={{ flex: 1, gap: 2 }}>
                <AppText variant="title">{b.title}</AppText>
                <AppText variant="caption" color="textMuted">{b.desc}</AppText>
              </View>
            </View>
          ))}
        </View>

        <Card tone="flat" padding="xxl" radius="lg" style={{ alignItems: 'center', gap: Spacing.sm, borderWidth: 1.5, borderColor: theme.primary + '40' }}>
          <AppText variant="caption" color="textSecondary">월 구독료</AppText>
          <AppText variant="display" tint={theme.primary}>10,000원</AppText>
          <AppText variant="caption" color="textMuted">/ 월</AppText>
        </Card>
      </ScrollView>

      <View style={{ padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        <Button label="구독하기" fullWidth size="lg" icon="star" onPress={() => router.push('/subscription/confirm')} />
      </View>
    </SafeAreaView>
  );
}
