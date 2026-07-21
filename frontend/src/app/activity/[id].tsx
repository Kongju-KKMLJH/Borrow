import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams } from 'expo-router';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { activitiesApi } from '@/api';
import { ImagePlaceholder } from '@/components/placeholder';
import { ScreenHeader } from '@/components/nav';
import { ActivityStatusBadge } from '@/components/status-badge';
import { VerifiedBadge } from '@/components/verified-badge';
import { AppText, Avatar, Badge, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { ActivityTypeLabel, DifficultyLabel, FieldLabel } from '@/data/types';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

export default function ActivityDetail() {
  const theme = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data: activity, loading } = useAsync(() => activitiesApi.getActivity(id!), [id]);

  if (loading || !activity) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }}>
        <ScreenHeader title="활동 상세" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader
        title="활동 상세"
        right={<Ionicons name="share-outline" size={22} color={theme.text} />}
      />
      <ScrollView contentContainerStyle={{ paddingBottom: Spacing.xxxl }} showsVerticalScrollIndicator={false}>
        {/* 히어로 */}
        <ImagePlaceholder field={activity.field} height={220} radius="sm" style={{ borderRadius: 0, marginHorizontal: 0 }}>
          <View style={{ position: 'absolute', top: Spacing.lg, left: Spacing.lg, flexDirection: 'row', gap: 6 }}>
            <Badge label={ActivityTypeLabel[activity.type]} tone="solid" />
            <Badge label={FieldLabel[activity.field]} tone="primary" />
          </View>
          <View style={{ position: 'absolute', top: Spacing.lg, right: Spacing.lg }}>
            <ActivityStatusBadge status={activity.status} />
          </View>
        </ImagePlaceholder>

        <View style={{ padding: Spacing.xl, gap: Spacing.xl }}>
          {/* 소개 */}
          <View style={{ gap: Spacing.sm }}>
            <AppText variant="h1">{activity.title}</AppText>
            <AppText variant="body" color="textSecondary">{activity.intro}</AppText>
          </View>

          {/* 진행자 */}
          <Card tone="muted" padding="lg" radius="lg" shadow="none" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
            <Avatar name={activity.host.name} uri={activity.host.avatar} size={44} />
            <View style={{ flex: 1, gap: 3 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                <AppText variant="title">{activity.host.name}</AppText>
                {activity.host.verifiedArtist && <VerifiedBadge size={17} />}
              </View>
              <AppText variant="caption" color="textMuted">{activity.host.bio}</AppText>
            </View>
            <Button label="포트폴리오" variant="outline" size="sm" />
          </Card>

          {/* 활동 정보 */}
          <Section title="활동 정보">
            <Row label="활동 날짜" value={activity.date} />
            <Row label="활동 시간" value={activity.time} />
            <Row label="모집 마감" value={activity.deadline} />
            <Row label="현재 인원" value={`${activity.joined} / ${activity.capacity}명`} />
            <Row label="난이도" value={DifficultyLabel[activity.difficulty]} last />
          </Section>

          {/* 공간 정보 */}
          {activity.space && (
            <Section title="공간 정보">
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md, paddingVertical: Spacing.sm }}>
                <ImagePlaceholder height={56} radius="md" style={{ width: 56 }} iconSize={22} />
                <View style={{ flex: 1, gap: 2 }}>
                  <AppText variant="title">{activity.space.name}</AppText>
                  <AppText variant="caption" color="textMuted">{activity.space.address}</AppText>
                </View>
              </View>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs, paddingTop: Spacing.sm }}>
                {activity.space.facilities.map((f) => (
                  <View key={f} style={{ backgroundColor: theme.surfaceMuted, paddingHorizontal: 10, paddingVertical: 6, borderRadius: 8 }}>
                    <AppText variant="small" color="textSecondary">{f}</AppText>
                  </View>
                ))}
              </View>
              {activity.space.notes && (
                <AppText variant="caption" color="textMuted" style={{ paddingTop: Spacing.sm }}>{activity.space.notes}</AppText>
              )}
            </Section>
          )}

          {/* 참가비 */}
          <Section title="참가비">
            <Row label="참가비" value={`${activity.fee.toLocaleString()}원`} />
            <Row label="공간 이용료" value="참가비 포함" />
            <Row label="재료비" value="참가비 포함" />
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingTop: Spacing.md }}>
              <AppText variant="body" color="textMuted">총 결제 금액</AppText>
              <AppText variant="h3" tint={theme.primary}>{activity.fee.toLocaleString()}원</AppText>
            </View>
          </Section>

          {/* 준비물 */}
          {activity.preparation && (
            <Section title="준비물 및 안내">
              {activity.preparation.map((p) => (
                <View key={p} style={{ flexDirection: 'row', gap: 8, alignItems: 'flex-start', paddingVertical: 4 }}>
                  <View style={{ width: 5, height: 5, borderRadius: 3, backgroundColor: theme.primary, marginTop: 8 }} />
                  <AppText variant="body" color="textSecondary" style={{ flex: 1 }}>{p}</AppText>
                </View>
              ))}
            </Section>
          )}
        </View>
      </ScrollView>

      {/* 하단 CTA */}
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.lg, padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        <View>
          <AppText variant="caption" color="textMuted">1인 참가비</AppText>
          <AppText variant="h3">{activity.fee.toLocaleString()}원</AppText>
        </View>
        <Button label="참여 신청하기" onPress={() => {}} style={{ flex: 1 }} />
      </View>
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
