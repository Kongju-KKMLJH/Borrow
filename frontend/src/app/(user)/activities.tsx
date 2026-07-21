import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { ScrollView, TextInput, View } from 'react-native';

import { ActivityCard } from '@/components/activity-card';
import { SelectChip } from '@/components/form';
import { AppText, Screen } from '@/components/ui';
import { FontFamily, Radius, Spacing } from '@/constants/theme';
import { activities } from '@/data/mock';
import type { ActivityType, FieldType } from '@/data/types';
import { useTheme } from '@/hooks/use-theme';

type Filter = '전체' | '취미 모임' | '전문 클래스' | '그림' | '촬영';
const FILTERS: Filter[] = ['전체', '취미 모임', '전문 클래스', '그림', '촬영'];
const TYPE: Partial<Record<Filter, ActivityType>> = { '취미 모임': 'hobby', '전문 클래스': 'class' };
const FIELD: Partial<Record<Filter, FieldType>> = { 그림: 'drawing', 촬영: 'photo' };

export default function Activities() {
  const theme = useTheme();
  const [filter, setFilter] = useState<Filter>('전체');
  const [query, setQuery] = useState('');

  const list = useMemo(() => {
    return activities.filter((a) => {
      if (TYPE[filter] && a.type !== TYPE[filter]) return false;
      if (FIELD[filter] && a.field !== FIELD[filter]) return false;
      if (query && !a.title.includes(query)) return false;
      return true;
    });
  }, [filter, query]);

  return (
    <Screen
      header={
        <View style={{ paddingHorizontal: Spacing.xl, paddingTop: Spacing.md, paddingBottom: Spacing.sm }}>
          <AppText variant="h2">활동 참여하기</AppText>
        </View>
      }
      contentContainerStyle={{ gap: Spacing.lg, paddingBottom: Spacing.huge }}>
      {/* 검색 */}
      <View style={{ paddingHorizontal: Spacing.xl }}>
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
      </View>

      {/* 필터 */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: Spacing.sm, paddingHorizontal: Spacing.xl }}>
        {FILTERS.map((f) => (
          <SelectChip key={f} label={f} active={filter === f} onPress={() => setFilter(f)} />
        ))}
      </ScrollView>

      {/* 결과 */}
      <View style={{ paddingHorizontal: Spacing.xl, flexDirection: 'row', justifyContent: 'space-between' }}>
        <AppText variant="caption" color="textMuted">총 {list.length}개 활동</AppText>
        <AppText variant="caption" color="textSecondary">추천순</AppText>
      </View>

      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        {list.map((a) => (
          <ActivityCard key={a.id} activity={a} onPress={() => router.push(`/activity/${a.id}`)} />
        ))}
        {list.length === 0 && (
          <View style={{ alignItems: 'center', paddingVertical: Spacing.huge, gap: Spacing.sm }}>
            <Ionicons name="search-outline" size={40} color={theme.textMuted} />
            <AppText variant="body" color="textMuted">조건에 맞는 활동이 없어요</AppText>
          </View>
        )}
      </View>
    </Screen>
  );
}
