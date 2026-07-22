import { Image } from 'expo-image';
import { View } from 'react-native';

import { Radius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

import { AppText } from './app-text';

/** 원형 아바타. uri 없으면 이니셜 표시. */
export function Avatar({
  uri,
  name,
  size = 40,
}: {
  uri?: string;
  name?: string;
  size?: number;
}) {
  const theme = useTheme();
  const initial = name?.trim().charAt(0).toUpperCase() ?? '?';
  return (
    <View
      style={{
        width: size,
        height: size,
        borderRadius: Radius.full,
        overflow: 'hidden',
        backgroundColor: theme.primarySoft,
        alignItems: 'center',
        justifyContent: 'center',
      }}>
      {uri ? (
        <Image source={{ uri }} style={{ width: size, height: size }} contentFit="cover" />
      ) : (
        <AppText variant="title" tint={theme.primary} style={{ fontSize: size * 0.4 }}>
          {initial}
        </AppText>
      )}
    </View>
  );
}
