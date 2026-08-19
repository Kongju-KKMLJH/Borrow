import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Pressable, View } from 'react-native';

import { imageUri } from '@/lib/api';
import { AppText, Avatar, Badge } from '@/components/ui';
import { ImagePlaceholder } from '@/components/placeholder';
import { ActivityStatusBadge } from '@/components/status-badge';
import { VerifiedBadge } from '@/components/verified-badge';
import { Radius, Spacing } from '@/constants/theme';
import type { ActivitySummaryResponse } from '@/lib/api/types';
import { ActivityFieldLabel, ActivityTypeLabel, formatCurrency, formatDate, formatTime, getDisplayActivityStatus } from '@/lib/format';
import { useTheme } from '@/hooks/use-theme';

export function ActivityCard({
  activity,
  onPress,
  variant = 'list',
}: {
  activity: ActivitySummaryResponse;
  onPress?: () => void;
  variant?: 'list' | 'rail';
}) {
  const theme = useTheme();
  const rail = variant === 'rail';
  const coverHeight = rail ? 130 : 150;
  const cover = activity.imageUrls?.[0];
  const displayStatus = getDisplayActivityStatus(activity);
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
      <View style={{ height: coverHeight }}>
        {cover ? (
          <Image source={{ uri: imageUri(cover) }} style={{ width: '100%', height: '100%', backgroundColor: theme.surfaceDeep }} contentFit="cover" transition={150} />
        ) : (
          <ImagePlaceholder field={activity.field} height={coverHeight} radius="sm" style={{ borderRadius: 0 }} />
        )}
        <View style={{ position: 'absolute', top: Spacing.sm, left: Spacing.sm }}>
          <Badge label={ActivityTypeLabel[activity.type]} tone="solid" />
        </View>
        <View style={{ position: 'absolute', top: Spacing.sm, right: Spacing.sm }}>
          <ActivityStatusBadge status={displayStatus} />
        </View>
      </View>

      <View style={{ padding: Spacing.md, gap: Spacing.sm }}>
        <Badge label={ActivityFieldLabel[activity.field]} tone="primary" />
        <AppText variant="h3" numberOfLines={2}>{activity.title}</AppText>

        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
          <Avatar name={activity.hostNickname ?? undefined} size={20} />
          <AppText variant="small" color="textSecondary">{activity.hostNickname}</AppText>
          {activity.hostCertified && <VerifiedBadge size={14} />}
        </View>

        <MetaRow icon="calendar-outline" text={`${formatDate(activity.date)} ${formatTime(activity.startTime)}`} />

        {activity.space && (
          <MetaRow icon="location-outline" text={activity.space.name} />
        )}

        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 2 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
            <AppText variant="caption" color="textSecondary">{activity.currentHeadcount} / {activity.capacity}명</AppText>
            {activity.status === 'PUBLISHED' && activity.remainingCapacity > 0 && (
              <View style={{ paddingHorizontal: 6, paddingVertical: 1, borderRadius: 4, backgroundColor: theme.primarySoft }}>
                <AppText variant="tiny" tint={theme.primary}>잔여 {activity.remainingCapacity}석</AppText>
              </View>
            )}
          </View>
          <AppText variant="title" tint={theme.primary}>{formatCurrency(activity.entryFee)}</AppText>
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
