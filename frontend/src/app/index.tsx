import { Redirect } from 'expo-router';

import { useAuth } from '@/lib/auth';
import { ActivityIndicator, View } from 'react-native';
import { useTheme } from '@/hooks/use-theme';

export default function Index() {
  const { user, loading } = useAuth();
  const theme = useTheme();

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: theme.background }}>
        <ActivityIndicator size="large" color={theme.primary} />
      </View>
    );
  }

  if (!user) return <Redirect href="/(auth)/login" />;
  // 관리자는 콘솔로 직행한다 (기능명세 7). 나머지 역할은 기존대로 사용자 홈에서 시작하고,
  // 공간 제공자 화면은 지금처럼 헤더의 모드 전환으로 넘어간다.
  if (user.role === 'ADMIN') return <Redirect href="/(admin)" />;
  return <Redirect href="/(user)" />;
}
