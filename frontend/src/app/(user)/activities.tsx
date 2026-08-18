import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { Pressable, ScrollView, TextInput, View } from 'react-native';

import { activityApi } from '@/lib/api';
import { ActivityCard } from '@/components/activity-card';
import { SelectChip } from '@/components/form';
import { AppText, Card, Screen } from '@/components/ui';
import { FontFamily, Radius, Spacing } from '@/constants/theme';
import type { ActivityField, ActivitySummaryResponse, ActivityType } from '@/lib/api/types';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

type Filter = '전체' | '취미 모임' | '전문 클래스' | '그림' | '촬영';
const FILTERS: Filter[] = ['전체', '취미 모임', '전문 클래스', '그림', '촬영'];
const TYPE: Partial<Record<Filter, ActivityType>> = { '취미 모임': 'HOBBY', '전문 클래스': 'CLASS' };
const FIELD: Partial<Record<Filter, ActivityField>> = { 그림: 'ART', 촬영: 'PHOTO' };

type Sort = '추천순' | '임박순' | '남은자리순';
const SORTS: Sort[] = ['추천순', '임박순', '남은자리순'];
const sortComparators: Record<Sort, ((a: ActivitySummaryResponse, b: ActivitySummaryResponse) => number) | null> = {
  추천순: null, // 서버 응답 순서 유지
  임박순: (a, b) => a.date.localeCompare(b.date) || a.startTime.localeCompare(b.startTime), // 활동일이 가까운 순
  남은자리순: (a, b) => b.remainingCapacity - a.remainingCapacity,
};

export default function Activities() {
  const theme = useTheme();
  const [filter, setFilter] = useState<Filter>('전체');
  const [query, setQuery] = useState('');
  const [region, setRegion] = useState('');
  const [sort, setSort] = useState<Sort>('추천순');
  const [sortOpen, setSortOpen] = useState(false);

  const { data: activities, loading } = useAsync(
    () => activityApi.list({ type: TYPE[filter], field: FIELD[filter], keyword: query || undefined, region: region || undefined }),
    [filter, query, region],
  );
  const list = useMemo(() => {
    const base = activities ?? [];
    const cmp = sortComparators[sort];
    return cmp ? [...base].sort(cmp) : base;
  }, [activities, sort]);

  return (
    <Screen
      header={
        <View style={{ paddingHorizontal: Spacing.xl, paddingTop: Spacing.md, paddingBottom: Spacing.sm }}>
          <AppText variant="h2">활동 참여하기</AppText>
        </View>
      }
      contentContainerStyle={{ gap: Spacing.lg, paddingBottom: Spacing.huge }}>
      {/* 검색 */}
      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.sm }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, backgroundColor: theme.surfaceMuted, borderRadius: Radius.md, paddingHorizontal: 14, paddingVertical: 12 }}>
          <Ionicons name="search" size={18} color={theme.textMuted} />
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder="활동명을 검색해보세요"
            placeholderTextColor={theme.textMuted}
            style={{ flex: 1, fontFamily: FontFamily.regular, fontSize: 15, color: theme.text, padding: 0 }}
          />
        </View>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, backgroundColor: theme.surfaceMuted, borderRadius: Radius.md, paddingHorizontal: 14, paddingVertical: 12 }}>
          <Ionicons name="location-outline" size={18} color={theme.textMuted} />
          <TextInput
            value={region}
            onChangeText={setRegion}
            placeholder="지역으로 검색 (예: 불당동, 천안시)"
            placeholderTextColor={theme.textMuted}
            style={{ flex: 1, fontFamily: FontFamily.regular, fontSize: 15, color: theme.text, padding: 0 }}
          />
          {region.length > 0 && (
            <Pressable hitSlop={8} onPress={() => setRegion('')}>
              <Ionicons name="close-circle" size={18} color={theme.textMuted} />
            </Pressable>
          )}
        </View>
      </View>

      {/* 필터 */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: Spacing.sm, paddingHorizontal: Spacing.xl }}>
        {FILTERS.map((f) => (
          <SelectChip key={f} label={f} active={filter === f} onPress={() => setFilter(f)} />
        ))}
      </ScrollView>

      {/* 결과 */}
      <View style={{ paddingHorizontal: Spacing.xl, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', zIndex: 10 }}>
        <AppText variant="caption" color="textMuted">총 {list.length}개 활동</AppText>
        <View>
          <Pressable
            onPress={() => setSortOpen((v) => !v)}
            hitSlop={8}
            style={{ flexDirection: 'row', alignItems: 'center', gap: 2 }}>
            <AppText variant="caption" color="textSecondary">{sort}</AppText>
            <Ionicons name={sortOpen ? 'chevron-up' : 'chevron-down'} size={14} color={theme.textSecondary} />
          </Pressable>
          {sortOpen && (
            <Card
              padding={0}
              radius="md"
              shadow="md"
              style={{ position: 'absolute', top: 24, right: 0, minWidth: 120, overflow: 'hidden' }}>
              {SORTS.map((s) => (
                <Pressable
                  key={s}
                  onPress={() => {
                    setSort(s);
                    setSortOpen(false);
                  }}
                  style={{ paddingVertical: 10, paddingHorizontal: 14, backgroundColor: s === sort ? theme.surfaceMuted : 'transparent' }}>
                  <AppText variant="caption" color={s === sort ? 'text' : 'textSecondary'}>{s}</AppText>
                </Pressable>
              ))}
            </Card>
          )}
        </View>
      </View>

      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        {list.map((a) => (
          <ActivityCard key={a.id} activity={a} onPress={() => router.push(`/activity/${a.id}`)} />
        ))}
        {!loading && list.length === 0 && (
          <View style={{ alignItems: 'center', paddingVertical: Spacing.huge, gap: Spacing.sm }}>
            <Ionicons name="search-outline" size={40} color={theme.textMuted} />
            <AppText variant="body" color="textMuted">조건에 맞는 활동이 없어요</AppText>
          </View>
        )}
      </View>
    </Screen>
  );
}
