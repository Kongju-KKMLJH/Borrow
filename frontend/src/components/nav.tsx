import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { Platform, Pressable, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppText } from '@/components/ui';
import { Radius, Shadow, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export type TabItem = { name: string; label: string; icon: keyof typeof Ionicons.glyphMap; active: keyof typeof Ionicons.glyphMap };

export const USER_TABS: TabItem[] = [
  { name: 'index', label: '홈', icon: 'home-outline', active: 'home' },
  { name: 'activities', label: '참여하기', icon: 'search-outline', active: 'search' },
  { name: 'create', label: '만들기', icon: 'add-circle-outline', active: 'add-circle' },
  { name: 'my-activities', label: '내 활동', icon: 'person-outline', active: 'person' },
];

export const PROVIDER_TABS: TabItem[] = [
  { name: 'index', label: '홈', icon: 'home-outline', active: 'home' },
  { name: 'requests', label: '요청', icon: 'documents-outline', active: 'documents' },
  { name: 'space', label: '공간 관리', icon: 'storefront-outline', active: 'storefront' },
];

type TabBarProps = {
  state: { index: number; routes: { key: string; name: string }[] };
  navigation: { emit: (...a: any[]) => any; navigate: (name: string) => void };
  items: TabItem[];
};

export function TabBar({ state, navigation, items }: TabBarProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  return (
    <View
      style={{
        flexDirection: 'row',
        paddingTop: Spacing.sm,
        paddingBottom: Math.max(insets.bottom, Spacing.sm),
        backgroundColor: theme.surface,
        borderTopWidth: Platform.OS === 'ios' ? 0 : 1,
        borderTopColor: theme.border,
        ...Shadow.md,
      }}>
      {state.routes.map((route, i) => {
        const item = items.find((t) => t.name === route.name);
        if (!item) return null;
        const focused = state.index === i;
        const onPress = () => {
          const e = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
          if (!focused && !e.defaultPrevented) navigation.navigate(route.name);
        };
        return (
          <Pressable key={route.key} onPress={onPress} style={{ flex: 1, alignItems: 'center', gap: 4 }}>
            <Ionicons name={focused ? item.active : item.icon} size={24} color={focused ? theme.primary : theme.textMuted} />
            <AppText variant="small" tint={focused ? theme.primary : theme.textMuted}>{item.label}</AppText>
          </Pressable>
        );
      })}
    </View>
  );
}

/** 모드 전환 알약 — 현재 모드 반대편으로 이동 */
export function ModeSwitch({ current }: { current: 'user' | 'provider' }) {
  const theme = useTheme();
  const target = current === 'user' ? 'provider' : 'user';
  const label = current === 'user' ? '공간 제공자' : '일반 사용자';
  return (
    <Pressable
      onPress={() => router.replace(target === 'provider' ? '/(provider)' : '/(user)')}
      style={{ flexDirection: 'row', alignItems: 'center', gap: 4, borderWidth: 1, borderColor: theme.border, backgroundColor: theme.surface, paddingHorizontal: Spacing.md, paddingVertical: 6, borderRadius: Radius.full }}>
      <Ionicons name="swap-horizontal" size={14} color={theme.textSecondary} />
      <AppText variant="small" color="textSecondary">{label}</AppText>
    </Pressable>
  );
}

/** 로고 + 우측 액션(모드 전환 등) 브랜드 헤더 */
export function BrandHeader({ right, subtitle }: { right?: React.ReactNode; subtitle?: string }) {
  const theme = useTheme();
  return (
    <View style={{ paddingHorizontal: Spacing.xl, paddingTop: Spacing.md, paddingBottom: Spacing.sm, gap: Spacing.sm }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
        <AppText variant="h2" tint={theme.primary}>Borrow</AppText>
        {right}
      </View>
      {subtitle ? <AppText variant="caption" color="textMuted">{subtitle}</AppText> : null}
    </View>
  );
}

/** 뒤로가기 + 제목 헤더 (상세/서브 화면) */
export function ScreenHeader({ title, right, onBack }: { title?: string; right?: React.ReactNode; onBack?: () => void }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, paddingHorizontal: Spacing.md, paddingTop: Spacing.md, paddingBottom: Spacing.sm }}>
      <Pressable onPress={onBack ?? (() => router.back())} hitSlop={8}>
        <Ionicons name="chevron-back" size={26} color={theme.text} />
      </Pressable>
      {title ? <AppText variant="title" style={{ flex: 1 }}>{title}</AppText> : <View style={{ flex: 1 }} />}
      {right}
    </View>
  );
}
