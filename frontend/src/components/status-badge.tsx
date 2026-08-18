import { View } from 'react-native';

import { AppText } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { ActivityStatusLabel, RequestStatusLabel } from '@/lib/format';
import type { ActivityStatus, RequestStatus } from '@/lib/api/types';
import { useTheme } from '@/hooks/use-theme';

type Tone = 'primary' | 'secondary' | 'accent' | 'neutral' | 'danger';

const ACTIVITY_TONE: Record<ActivityStatus, Tone> = {
  DRAFT: 'neutral', PENDING: 'accent', PUBLISHED: 'secondary', REJECTED: 'danger',
};
const REQUEST_TONE: Record<RequestStatus, Tone> = { PENDING: 'accent', APPROVED: 'secondary', REJECTED: 'danger' };

function Dot({ label, tone }: { label: string; tone: Tone }) {
  const theme = useTheme();
  const map: Record<Tone, { bg: string; fg: string }> = {
    primary: { bg: theme.primarySoft, fg: theme.primary },
    secondary: { bg: theme.secondarySoft, fg: theme.secondary },
    accent: { bg: theme.accentSoft, fg: theme.accentPressed },
    neutral: { bg: theme.surfaceMuted, fg: theme.textSecondary },
    danger: { bg: theme.dangerSoft, fg: theme.danger },
  };
  const c = map[tone];
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 5, alignSelf: 'flex-start', backgroundColor: c.bg, paddingHorizontal: Spacing.sm, paddingVertical: 4, borderRadius: Radius.full }}>
      <View style={{ width: 5, height: 5, borderRadius: 3, backgroundColor: c.fg }} />
      <AppText variant="tiny" tint={c.fg}>{label}</AppText>
    </View>
  );
}

export function ActivityStatusBadge({ status }: { status: ActivityStatus }) {
  return <Dot label={ActivityStatusLabel[status]} tone={ACTIVITY_TONE[status]} />;
}

export function RequestStatusBadge({ status }: { status: RequestStatus }) {
  return <Dot label={RequestStatusLabel[status]} tone={REQUEST_TONE[status]} />;
}
