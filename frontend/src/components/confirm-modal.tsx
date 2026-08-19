import { Modal, Pressable, View } from 'react-native';

import { AppText, Button } from '@/components/ui';
import { Radius, Shadow, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

/**
 * 되돌릴 수 없는 조치를 실행하기 전 대상과 결과를 보여주는 확인 모달.
 * 관리자 콘솔의 강제 탈퇴·강제 삭제가 요구하는 확인 단계다 (기능명세 7.1.3 · 7.2.3 · 7.3.3).
 *
 * <p>{@code Alert.alert} 을 쓰지 않는다 — 웹에서 동작하지 않고, 배포는 앱과 웹을
 * 같은 코드로 낸다. 대상 정보와 안내 문구를 함께 보여줘야 한다는 요구도 Alert 로는 좁다.
 */
export function ConfirmModal({
  visible,
  title,
  target,
  message,
  confirmLabel = '실행',
  loading,
  onConfirm,
  onCancel,
}: {
  visible: boolean;
  title: string;
  /** 조치 대상 — 잘못된 대상에 실행하지 않도록 이름을 그대로 보여준다 */
  target?: string;
  /** 실행하면 무엇이 차단·중지되는지 */
  message: string;
  confirmLabel?: string;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const theme = useTheme();

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <Pressable
        onPress={onCancel}
        style={{
          flex: 1,
          backgroundColor: 'rgba(0,0,0,0.45)',
          justifyContent: 'center',
          padding: Spacing.xl,
        }}>
        {/* 카드 안쪽 탭이 배경으로 전파돼 모달이 닫히지 않게 막는다 */}
        <Pressable
          onPress={(e) => e.stopPropagation()}
          style={{
            backgroundColor: theme.surface,
            borderRadius: Radius.lg,
            padding: Spacing.xl,
            gap: Spacing.md,
            width: '100%',
            maxWidth: 420,
            alignSelf: 'center',
            ...Shadow.md,
          }}>
          <AppText variant="h3">{title}</AppText>

          {target ? (
            <View
              style={{
                backgroundColor: theme.surfaceMuted,
                borderRadius: Radius.md,
                paddingHorizontal: Spacing.md,
                paddingVertical: Spacing.sm,
              }}>
              <AppText variant="body">{target}</AppText>
            </View>
          ) : null}

          <AppText variant="caption" color="textMuted">
            {message}
          </AppText>

          <View style={{ flexDirection: 'row', gap: Spacing.sm, marginTop: Spacing.sm }}>
            <Button label="취소" variant="outline" size="sm" onPress={onCancel} style={{ flex: 1 }} />
            <Button
              label={confirmLabel}
              variant="primary"
              size="sm"
              loading={loading}
              onPress={onConfirm}
              style={{ flex: 1 }}
            />
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
