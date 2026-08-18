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
  return <Redirect href="/(user)" />;
}
