import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { requestsApi } from '@/api';
import { ScreenHeader } from '@/components/nav';
import { RequestStatusBadge } from '@/components/status-badge';
import { AppText, Avatar, Badge, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { ActivityTypeLabel, DifficultyLabel, FieldLabel } from '@/data/types';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

const CHECK_FACILITIES = ['넓은 테이블·좌석', '자연광', '물 사용', '콘센트', '촬영 조명', '세면시설'];

export default function RequestDetail() {
  const theme = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data: req, loading } = useAsync(() => requestsApi.getRequest(id!), [id]);

  if (loading || !req) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }}>
        <ScreenHeader title="개최 요청 상세" />
      </SafeAreaView>
    );
  }
  const { activity } = req;
  const have = new Set(activity.space?.facilities ?? []);
  const facilityChecks = CHECK_FACILITIES.map((f) => ({ label: f, ok: [...have].some((h) => f.includes(h) || h.includes(f.split('·')[0])) }));

  const approve = async () => { await requestsApi.approveRequest(req.id); router.back(); };
  const reject = async () => { await requestsApi.rejectRequest(req.id, ''); router.back(); };

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="개최 요청 상세" />
      <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.xl, paddingBottom: Spacing.xxxl }} showsVerticalScrollIndicator={false}>
        {/* 활동 정보 */}
        <View style={{ gap: Spacing.md }}>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
            <Badge label={ActivityTypeLabel[activity.type]} tone="neutral" />
            <Badge label={FieldLabel[activity.field]} tone="primary" />
            <View style={{ flex: 1 }} />
            <RequestStatusBadge status={req.status} />
          </View>
          <AppText variant="h1">{activity.title}</AppText>
          <AppText variant="body" color="textSecondary">{activity.intro}</AppText>
          <Card tone="muted" padding="lg" radius="lg" shadow="none" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
            <Avatar name={req.requester.name} size={36} />
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <AppText variant="title">{req.requester.name}</AppText>
              {req.requester.verifiedArtist && <Badge label="인증 예술가" tone="accent" />}
            </View>
          </Card>
        </View>

        <Section title="일정">
          <Row label="활동 날짜" value={activity.date} />
          <Row label="활동 시간" value={activity.time} />
          <Row label="공간 사용 예정" value={req.spaceUseTime} last />
        </Section>

        <Section title="참여">
          <Row label="모집 인원" value={`${req.headcount}명`} />
          <Row label="난이도" value={DifficultyLabel[activity.difficulty]} last />
        </Section>

        {/* 요청 시설 */}
        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">요청 시설</AppText>
          <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
            {facilityChecks.map((f) => (
              <View key={f.label} style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                <Ionicons name={f.ok ? 'checkmark-circle' : 'close-circle'} size={18} color={f.ok ? theme.secondary : theme.textMuted} />
                <AppText variant="body" color={f.ok ? 'text' : 'textMuted'}>{f.label}</AppText>
              </View>
            ))}
          </Card>
        </View>

        {/* AI 매칭 */}
        <View style={{ gap: Spacing.sm }}>
          <AppText variant="h3">AI 매칭 분석</AppText>
          <View style={{ backgroundColor: theme.secondarySoft, borderRadius: 16, padding: Spacing.lg, gap: Spacing.md }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
              <AppText variant="title">우리 공간과의 적합도</AppText>
              <AppText variant="h1" tint={theme.secondary}>{req.matchScore}%</AppText>
            </View>
            {req.matched.map((m) => (
              <View key={m} style={{ flexDirection: 'row', gap: 6 }}>
                <Ionicons name="checkmark-circle" size={16} color={theme.secondary} style={{ marginTop: 1 }} />
                <AppText variant="caption" color="textSecondary" style={{ flex: 1 }}>{m}</AppText>
              </View>
            ))}
            {req.cautions.map((c) => (
              <View key={c} style={{ flexDirection: 'row', gap: 6 }}>
                <Ionicons name="warning" size={16} color={theme.accent} style={{ marginTop: 1 }} />
                <AppText variant="caption" color="textSecondary" style={{ flex: 1 }}>{c}</AppText>
              </View>
            ))}
          </View>
        </View>
      </ScrollView>

      {/* 승인/거절 */}
      {req.status === 'pending' && (
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
