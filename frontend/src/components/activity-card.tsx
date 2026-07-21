import { Ionicons } from '@expo/vector-icons';
import { Pressable, View } from 'react-native';

import { AppText, Avatar, Badge } from '@/components/ui';
import { ImagePlaceholder } from '@/components/placeholder';
import { ActivityStatusBadge } from '@/components/status-badge';
import { Radius, Spacing } from '@/constants/theme';
import { ActivityTypeLabel, FieldLabel, type Activity } from '@/data/types';
import { useTheme } from '@/hooks/use-theme';

export function ActivityCard({
  activity,
  onPress,
  variant = 'list',
}: {
  activity: Activity;
  onPress?: () => void;
  variant?: 'list' | 'rail';
}) {
  const theme = useTheme();
  const rail = variant === 'rail';
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => ({
        width: rail ? 250 : undefined,
        alignSelf: rail ? undefined : 'stretch',
        borderRadius: Radius.lg,
        borderWidth: 1,
        borderColor: theme.border,
        backgroundColor: theme.surface,
        overflow: 'hidden',
        opacity: pressed ? 0.9 : 1,
      })}>
      <ImagePlaceholder field={activity.field} height={rail ? 130 : 150} radius="sm" style={{ borderRadius: 0 }}>
        <View style={{ position: 'absolute', top: Spacing.sm, left: Spacing.sm }}>
          <Badge label={ActivityTypeLabel[activity.type]} tone="solid" />
        </View>
        <View style={{ position: 'absolute', top: Spacing.sm, right: Spacing.sm }}>
          <ActivityStatusBadge status={activity.status} />
        </View>
      </ImagePlaceholder>

      <View style={{ padding: Spacing.md, gap: Spacing.sm }}>
        <Badge label={FieldLabel[activity.field]} tone="primary" />
        <AppText variant="h3" numberOfLines={2}>{activity.title}</AppText>

        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
          <Avatar name={activity.host.name} uri={activity.host.avatar} size={20} />
          <AppText variant="small" color="textSecondary">{activity.host.name}</AppText>
          {activity.host.verifiedArtist && <Badge label="인증 예술가" tone="accent" />}
        </View>

        <MetaRow icon="calendar-outline" text={`${activity.date} ${activity.time.split(' ~ ')[0] ?? ''}`} />
        <MetaRow icon="location-outline" text={`${activity.space?.name ?? activity.region}`} />

        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 2 }}>
          <AppText variant="caption" color="textSecondary">{activity.joined} / {activity.capacity}명</AppText>
          <AppText variant="title" tint={theme.primary}>{activity.fee.toLocaleString()}원</AppText>
        </View>
      </View>
    </Pressable>
  );
}

function MetaRow({ icon, text }: { icon: keyof typeof Ionicons.glyphMap; text: string }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 5 }}>
      <Ionicons name={icon} size={13} color={theme.textMuted} />
      <AppText variant="caption" color="textMuted" numberOfLines={1} style={{ flex: 1 }}>{text}</AppText>
    </View>
  );
}
