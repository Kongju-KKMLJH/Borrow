import { Ionicons } from '@expo/vector-icons';
import { Pressable, View } from 'react-native';

import { AppText, Badge, Button, Card } from '@/components/ui';
import { RequestStatusBadge } from '@/components/status-badge';
import { Spacing } from '@/constants/theme';
import { ActivityTypeLabel, FieldLabel, type HostingRequest } from '@/data/types';
import { useTheme } from '@/hooks/use-theme';

export function RequestCard({
  request,
  onPress,
  onApprove,
  onReject,
}: {
  request: HostingRequest;
  onPress?: () => void;
  onApprove?: () => void;
  onReject?: () => void;
}) {
  const theme = useTheme();
  const { activity, requester, requestedTime, headcount, status } = request;
  const showActions = status === 'pending' && (onApprove || onReject);
  return (
    <Card padding="lg" radius="lg" tone="flat" style={{ gap: Spacing.md }} onPress={onPress}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
        <View style={{ flexDirection: 'row', gap: 6, flex: 1 }}>
          <Badge label={ActivityTypeLabel[activity.type]} tone="neutral" />
          <Badge label={FieldLabel[activity.field]} tone="primary" />
        </View>
        <RequestStatusBadge status={status} />
      </View>

      <AppText variant="h3" numberOfLines={2}>{activity.title}</AppText>

      <Meta icon="calendar-outline" text={requestedTime} />
      <Meta icon="people-outline" text={`진행자 ${requester.name} · 요청 인원 ${headcount}명`} />

      {showActions && (
        <View style={{ flexDirection: 'row', gap: Spacing.sm, marginTop: 2 }}>
          <Button label="거절" variant="outline" onPress={onReject} style={{ flex: 1 }} />
          <Button label="승인" onPress={onApprove} style={{ flex: 1 }} />
        </View>
      )}
    </Card>
  );
}

function Meta({ icon, text }: { icon: keyof typeof Ionicons.glyphMap; text: string }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
      <Ionicons name={icon} size={14} color={theme.textMuted} />
      <AppText variant="caption" color="textSecondary" style={{ flex: 1 }}>{text}</AppText>
    </View>
  );
}
