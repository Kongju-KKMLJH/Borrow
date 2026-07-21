import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useEffect, useRef } from 'react';
import { Animated, StyleSheet, View } from 'react-native';

import { AppText } from '@/components/ui';
import { Spacing } from '@/constants/theme';

/**
 * 인앱 스플래시 — 핑크→퍼플 대각선 그라데이션 위 팔레트 로고 + "아트민".
 * 잠깐 노출 후 페이드아웃하며 onFinish를 호출한다.
 * (네이티브 config 스플래시는 그라데이션을 못 그리므로, JS에서 디자인을 재현한다.)
 */
export function AnimatedSplash({ onFinish }: { onFinish: () => void }) {
  const opacity = useRef(new Animated.Value(1)).current;
  const logo = useRef(new Animated.Value(0.92)).current;

  useEffect(() => {
    Animated.spring(logo, { toValue: 1, friction: 6, tension: 40, useNativeDriver: true }).start();
    const t = setTimeout(() => {
      Animated.timing(opacity, { toValue: 0, duration: 420, useNativeDriver: true }).start(({ finished }) => {
        if (finished) onFinish();
      });
    }, 1300);
    return () => clearTimeout(t);
  }, [onFinish, opacity, logo]);

  return (
    <Animated.View style={[StyleSheet.absoluteFill, { opacity, zIndex: 100 }]} pointerEvents="none">
      <LinearGradient
        colors={['#C879D6', '#E98AAD', '#F2687C']}
        start={{ x: 0.9, y: 0 }}
        end={{ x: 0.1, y: 1 }}
        style={StyleSheet.absoluteFill}
      />
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <Animated.View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md, transform: [{ scale: logo }] }}>
          <Ionicons name="color-palette" size={46} color="#FFFFFF" />
          <AppText variant="display" tint="#FFFFFF" style={{ fontSize: 40, lineHeight: 48 }}>
            아트민
          </AppText>
        </Animated.View>
      </View>
    </Animated.View>
  );
}
