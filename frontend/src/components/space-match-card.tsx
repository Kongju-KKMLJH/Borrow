import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { View } from 'react-native';

import { imageUri } from '@/api';
import { AppText, Badge, Button, Card } from '@/components/ui';
import { ImagePlaceholder } from '@/components/placeholder';
import { Radius, Spacing } from '@/constants/theme';
import type { SpaceMatch } from '@/data/types';
import { useTheme } from '@/hooks/use-theme';

export function SpaceMatchCard({ match, onSelect }: { match: SpaceMatch; onSelect?: () => void }) {
  const theme = useTheme();
  const { name, region, capacity, score, reason, aiScored, imageUrls } = match;
  const cover = imageUrls?.[0];
  return (
    <Card padding="lg" radius="lg" tone="flat" style={{ gap: Spacing.md }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
        {cover ? (
          <Image source={{ uri: imageUri(cover) }} style={{ width: 60, height: 60, borderRadius: Radius.md, backgroundColor: theme.surfaceMuted }} contentFit="cover" transition={200} />
        ) : (
          <ImagePlaceholder height={60} radius="md" style={{ width: 60 }} iconSize={24} />
        )}
        <View style={{ flex: 1, gap: 2 }}>
          <AppText variant="h3" numberOfLines={1}>{name}</AppText>
          <AppText variant="caption" color="textMuted">{region} · 최대 {capacity}명</AppText>
        </View>
        <View style={{ alignItems: 'center', backgroundColor: theme.secondarySoft, paddingHorizontal: Spacing.md, paddingVertical: 6, borderRadius: Radius.md }}>
          <AppText variant="h3" tint={theme.secondary}>{score}%</AppText>
          <AppText variant="tiny" tint={theme.secondary}>적합도</AppText>
        </View>
      </View>

      <View style={{ flexDirection: 'row', gap: Spacing.sm, backgroundColor: theme.secondarySoft, padding: Spacing.md, borderRadius: Radius.md }}>
        <Ionicons name="bulb-outline" size={16} color={theme.secondary} style={{ marginTop: 1 }} />
        <AppText variant="caption" color="textSecondary" style={{ flex: 1 }}>{reason}</AppText>
      </View>

      {!aiScored && <Badge label="AI 응답 실패 · 규칙 기반 추천" tone="neutral" />}

      <Button label="이 공간 선택" onPress={onSelect} fullWidth />
    </Card>
  );
}
