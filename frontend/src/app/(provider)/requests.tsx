import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, View } from 'react-native';

import { hostApi } from '@/api';
import { SelectChip } from '@/components/form';
import { RequestCard } from '@/components/request-card';
import { AppText, Screen } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import type { RequestStatus } from '@/data/types';
import { useAsync } from '@/hooks/use-async';

const FILTERS: { label: string; value?: RequestStatus }[] = [
  { label: '전체' }, { label: '승인 대기', value: 'PENDING' }, { label: '승인', value: 'APPROVED' }, { label: '거절', value: 'REJECTED' },
];

export default function Requests() {
  const [idx, setIdx] = useState(0);
  const status = FILTERS[idx].value;
  const { data: list } = useAsync(() => hostApi.listRequests({ status }), [status], { refetchOnFocus: true });

  return (
    <Screen
      header={
        <View style={{ paddingHorizontal: Spacing.xl, paddingTop: Spacing.md, paddingBottom: Spacing.sm }}>
          <AppText variant="h2">개최 요청</AppText>
        </View>
      }
      contentContainerStyle={{ gap: Spacing.lg, paddingBottom: Spacing.huge }}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: Spacing.sm, paddingHorizontal: Spacing.xl }}>
        {FILTERS.map((f, i) => (
          <SelectChip key={f.label} label={f.label} active={idx === i} onPress={() => setIdx(i)} />
        ))}
      </ScrollView>

      <View style={{ paddingHorizontal: Spacing.xl, gap: Spacing.md }}>
        {(list ?? []).map((r) => (
          <RequestCard key={r.id} request={r} onPress={() => router.push(`/request/${r.id}`)} onApprove={() => router.push(`/request/${r.id}`)} onReject={() => router.push(`/request/${r.id}`)} />
        ))}
      </View>
    </Screen>
  );
}
