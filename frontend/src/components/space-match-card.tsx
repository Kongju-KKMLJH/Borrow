import { Ionicons } from '@expo/vector-icons';
import { View } from 'react-native';

import { AppText, Button, Card } from '@/components/ui';
import { ImagePlaceholder } from '@/components/placeholder';
import { Radius, Spacing } from '@/constants/theme';
import type { SpaceMatch } from '@/data/types';
import { useTheme } from '@/hooks/use-theme';

export function SpaceMatchCard({ match, onSelect }: { match: SpaceMatch; onSelect?: () => void }) {
  const theme = useTheme();
  const { space, score, reason, matched, cautions } = match;
  return (
    <Card padding="lg" radius="lg" tone="flat" style={{ gap: Spacing.md }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
        <ImagePlaceholder height={60} radius="md" style={{ width: 60 }} iconSize={24} />
        <View style={{ flex: 1, gap: 2 }}>
          <AppText variant="h3" numberOfLines={1}>{space.name}</AppText>
          <AppText variant="caption" color="textMuted">{space.district} · 최대 {space.capacity}명</AppText>
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

      {matched.map((m) => <Cond key={m} icon="checkmark-circle" color={theme.secondary} text={m} />)}
      {cautions.map((c) => <Cond key={c} icon="warning" color={theme.accent} text={c} />)}

      <Button label="이 공간 선택" onPress={onSelect} fullWidth />
    </Card>
  );
}

function Cond({ icon, color, text }: { icon: keyof typeof Ionicons.glyphMap; color: string; text: string }) {
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
      <Ionicons name={icon} size={16} color={color} />
      <AppText variant="caption" color="textSecondary" style={{ flex: 1 }}>{text}</AppText>
    </View>
  );
}
