import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { View } from 'react-native';

import { requestsApi, spacesApi } from '@/api';
import { ImagePlaceholder } from '@/components/placeholder';
import { BrandHeader, ModeSwitch } from '@/components/nav';
import { RequestCard } from '@/components/request-card';
import { AppText, Button, Card, Screen, SectionHeader } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { currentUser, providerSummary } from '@/data/mock';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

export default function ProviderHome() {
  const theme = useTheme();
  const { data: requests } = useAsync(() => requestsApi.listRequests('pending'));
  const { data: space } = useAsync(() => spacesApi.getMySpace());

  return (
    <Screen
      header={<BrandHeader right={<ModeSwitch current="provider" />} subtitle={`${currentUser.businessName} · 사장님, 안녕하세요 👋`} />}
      contentContainerStyle={{ gap: Spacing.xxl, paddingBottom: Spacing.huge }}>
      {/* 요약 히어로 (프라이머리) */}
      <View style={{ paddingHorizontal: Spacing.xl }}>
        <View style={{ backgroundColor: theme.primary, borderRadius: Radius.xl, padding: Spacing.xl, gap: Spacing.lg }}>
          <AppText variant="label" tint="#FFFFFF" style={{ opacity: 0.85 }}>이번 주 공간 현황</AppText>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Stat n={providerSummary.pending} label="승인 대기" />
            <Divider />
            <Stat n={providerSummary.approved} label="승인 활동" />
            <Divider />
            <Stat n={providerSummary.upcoming} label="예정 활동" />
          </View>
        </View>
      </View>

      {/* 내 공간 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        <AppText variant="h3">내 공간</AppText>
        <Card tone="flat" padding="md" radius="lg" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
          <ImagePlaceholder height={64} radius="md" style={{ width: 64 }} iconSize={24} />
          <View style={{ flex: 1, gap: 3 }}>
            <AppText variant="title">{space?.name}</AppText>
            <AppText variant="caption" color="textMuted">{space?.district} · 최대 {space?.capacity}명</AppText>
          </View>
          <Button label="수정" variant="outline" size="sm" onPress={() => router.push('/(provider)/space')} />
        </Card>
      </View>

      {/* 새로운 개최 요청 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        <SectionHeader title="새로운 개최 요청" actionLabel="전체 보기" onAction={() => router.push('/(provider)/requests')} style={{ marginBottom: 0 }} />
        {(requests ?? []).map((r) => (
          <RequestCard key={r.id} request={r} onPress={() => router.push(`/request/${r.id}`)} onApprove={() => router.push(`/request/${r.id}`)} onReject={() => router.push(`/request/${r.id}`)} />
        ))}
      </View>

      {/* 예정된 활동 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        <AppText variant="h3">예정된 활동</AppText>
        <Card tone="flat" padding="lg" radius="lg" style={{ gap: 0 }}>
          <Upcoming title="캘리그래피 엽서 만들기" date="10월 20일 (일) 오후 3:00" people="6명" />
          <Upcoming title="원데이 유화 클래스" date="10월 22일 (화) 오전 11:00" people="4명" last />
        </Card>
      </View>
    </Screen>
  );
}

function Stat({ n, label }: { n: number; label: string }) {
  return (
    <View style={{ flex: 1, alignItems: 'center', gap: 4 }}>
      <AppText variant="h1" tint="#FFFFFF">{n}</AppText>
      <AppText variant="caption" tint="#FFFFFF" style={{ opacity: 0.85 }}>{label}</AppText>
    </View>
  );
}
function Divider() {
  return <View style={{ width: 1, height: 34, backgroundColor: '#FFFFFF', opacity: 0.25 }} />;
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
