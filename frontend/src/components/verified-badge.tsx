import { Ionicons } from '@expo/vector-icons';
import { View } from 'react-native';

import { useTheme } from '@/hooks/use-theme';

/**
 * 인증 예술가 배지 — 인스타그램식 인증 체크.
 * 틸(브랜드 secondary) 원 + 흰색 체크. 호스트 이름 옆에 사용.
 */
export function VerifiedBadge({ size = 16 }: { size?: number }) {
  const theme = useTheme();
  return (
    <View
      style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        backgroundColor: theme.secondary,
        alignItems: 'center',
        justifyContent: 'center',
      }}>
      <Ionicons name="checkmark-sharp" size={size * 0.7} color="#FFFFFF" />
    </View>
  );
}
