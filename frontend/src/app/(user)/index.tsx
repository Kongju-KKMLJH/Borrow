import { router } from 'expo-router';
import { ScrollView, View } from 'react-native';

import { activityApi } from '@/lib/api';
import { ActivityCard } from '@/components/activity-card';
import { SelectChip } from '@/components/form';
import { BrandHeader, LogoutButton, ModeSwitch } from '@/components/nav';
import { AppText, Button, Card, Screen, SectionHeader } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useAsync } from '@/hooks/use-async';

const CATEGORIES = ['취미 모임', '전문 클래스', '그림', '촬영'];

export default function UserHome() {
  const { data: activities } = useAsync(() => activityApi.list(), [], { refetchOnFocus: true });
  const list = activities ?? [];

  return (
      <Screen header={<BrandHeader right={<View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}><ModeSwitch current="user" /><LogoutButton /></View>} />} contentContainerStyle={{ gap: Spacing.xxxl, paddingBottom: Spacing.huge }}>
      {/* 히어로 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.lg }}>
        <View style={{ gap: Spacing.sm }}>
          <AppText variant="label" color="primary">천안 · 취미 모임</AppText>
          <AppText variant="display" style={{ fontSize: 30, lineHeight: 40 }}>함께할 취미,{'\n'}가까운 공간에서</AppText>
          <AppText variant="body" color="textSecondary">동네 유휴 공간에서 열리는 취미 모임과 클래스를 만나보세요.</AppText>
        </View>
        <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
          <Button label="활동 참여하기" onPress={() => router.push('/(user)/activities')} style={{ flex: 1 }} />
          <Button label="활동 만들기" variant="outline" onPress={() => router.push('/(user)/create')} style={{ flex: 1 }} />
        </View>
      </View>

      {/* 카테고리 */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: Spacing.sm, paddingHorizontal: Spacing.xl }}>
        {CATEGORIES.map((c, i) => (
          <SelectChip key={c} label={c} active={i === 0} onPress={() => router.push('/(user)/activities')} />
        ))}
      </ScrollView>

      {/* 추천 활동 */}
      <View style={{ gap: Spacing.md }}>
        <SectionHeader title="추천 활동" actionLabel="전체보기" onAction={() => router.push('/(user)/activities')} style={{ paddingHorizontal: Spacing.xl, marginBottom: 0 }} />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: Spacing.md, paddingHorizontal: Spacing.xl }}>
          {list.map((a) => (
            <ActivityCard key={a.id} activity={a} variant="rail" onPress={() => router.push(`/activity/${a.id}`)} />
          ))}
        </ScrollView>
      </View>

      {/* 천안 지역 활동 */}
      <View style={{ gap: Spacing.md }}>
        <SectionHeader title="천안 지역 활동" style={{ paddingHorizontal: Spacing.xl, marginBottom: 0 }} />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: Spacing.md, paddingHorizontal: Spacing.xl }}>
          {[...list].reverse().map((a) => (
            <ActivityCard key={a.id} activity={a} variant="rail" onPress={() => router.push(`/activity/${a.id}`)} />
          ))}
        </ScrollView>
      </View>

      {/* 만들기 CTA */}
      <View style={{ paddingHorizontal: Spacing.xl }}>
        <Card tone="muted" padding="xl" radius="lg" style={{ alignItems: 'center', gap: Spacing.md }}>
          <AppText variant="title">원하는 활동이 없나요?</AppText>
          <AppText variant="body" color="textSecondary">직접 취미 모임을 만들어보세요</AppText>
          <Button label="활동 만들기" fullWidth onPress={() => router.push('/(user)/create')} />
        </Card>
      </View>

      <View style={{ paddingHorizontal: Spacing.xl }}>
        <Button label="예술가 인증 신청" variant="outline" fullWidth onPress={() => router.push('/artist-verification')} />
      </View>
    </Screen>
  );
}
