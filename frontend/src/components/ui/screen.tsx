import { ScrollView, View, type ScrollViewProps } from 'react-native';
import { SafeAreaView, type Edge } from 'react-native-safe-area-context';

import { MaxContentWidth, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

type ScreenProps = ScrollViewProps & {
  scroll?: boolean;
  edges?: Edge[];
  /** 하단 여백 (탭바/CTA 공간 확보) */
  bottomInset?: number;
  header?: React.ReactNode;
};

/**
 * 화면 공통 래퍼. SafeArea + 배경색 + (옵션) 스크롤 + 최대 너비 제한.
 */
export function Screen({
  scroll = true,
  edges = ['top'],
  bottomInset = Spacing.xxxl,
  header,
  children,
  contentContainerStyle,
  ...rest
}: ScreenProps) {
  const theme = useTheme();
  const body = scroll ? (
    <ScrollView
      showsVerticalScrollIndicator={false}
      contentContainerStyle={[
        { paddingBottom: bottomInset, width: '100%', maxWidth: MaxContentWidth, alignSelf: 'center' },
        contentContainerStyle,
      ]}
      {...rest}>
      {children}
    </ScrollView>
  ) : (
    <View style={{ flex: 1, width: '100%', maxWidth: MaxContentWidth, alignSelf: 'center' }}>
      {children}
    </View>
  );

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={edges}>
      {header}
      {body}
    </SafeAreaView>
  );
}
