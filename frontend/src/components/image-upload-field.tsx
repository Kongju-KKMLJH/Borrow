import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import * as ImagePicker from 'expo-image-picker';
import { useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, View } from 'react-native';

import { imageUri, uploadApi } from '@/lib/api';
import { Field } from '@/components/form';
import { AppText } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

/**
 * 이미지 등록 필드 — 갤러리 다중 선택 / 카메라 촬영 → 서버 업로드 → 상대경로 URL 배열 관리.
 * value/onChange는 서버 상대경로("/files/a.jpg") 배열을 주고받는다.
 */
export function ImageUploadField({
  label,
  value,
  onChange,
  max = 5,
}: {
  label: string;
  value: string[];
  onChange: (urls: string[]) => void;
  max?: number;
}) {
  const theme = useTheme();
  const [busy, setBusy] = useState(false);
  const remaining = max - value.length;

  const upload = async (uris: string[]) => {
    if (uris.length === 0) return;
    setBusy(true);
    try {
      const { urls } = await uploadApi.upload(uris.slice(0, remaining));
      onChange([...value, ...urls]);
    } catch (e) {
      Alert.alert('업로드 실패', e instanceof Error ? e.message : '이미지 업로드에 실패했어요.');
    } finally {
      setBusy(false);
    }
  };

  const pickGallery = async () => {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) return Alert.alert('권한 필요', '사진 접근 권한을 허용해주세요.');
    const r = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsMultipleSelection: true, quality: 1 });
    if (!r.canceled) await upload(r.assets.map((a) => a.uri));
  };

  const takePhoto = async () => {
    const perm = await ImagePicker.requestCameraPermissionsAsync();
    if (!perm.granted) return Alert.alert('권한 필요', '카메라 권한을 허용해주세요.');
    const r = await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 1 });
    if (!r.canceled) await upload(r.assets.map((a) => a.uri));
  };

  return (
    <Field label={label}>
      {value.length > 0 && (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: Spacing.sm }}>
          {value.map((url, i) => (
            <View key={`${url}-${i}`}>
              <Image
                source={{ uri: imageUri(url) }}
                style={{ width: 96, height: 96, borderRadius: Radius.md, backgroundColor: theme.surfaceMuted }}
                contentFit="cover"
                transition={150}
              />
              <Pressable
                onPress={() => onChange(value.filter((_, idx) => idx !== i))}
                hitSlop={6}
                style={{ position: 'absolute', top: 4, right: 4, width: 22, height: 22, borderRadius: 11, backgroundColor: 'rgba(0,0,0,0.55)', alignItems: 'center', justifyContent: 'center' }}>
                <Ionicons name="close" size={14} color="#FFFFFF" />
              </Pressable>
            </View>
          ))}
        </ScrollView>
      )}

      {remaining > 0 && (
        <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
          <PickButton icon="images-outline" label="갤러리" busy={busy} onPress={pickGallery} />
          <PickButton icon="camera-outline" label="촬영" busy={busy} onPress={takePhoto} />
        </View>
      )}

      <AppText variant="tiny" color="textMuted">최대 {max}장 · 현재 {value.length}장</AppText>
    </Field>
  );
}

function PickButton({ icon, label, busy, onPress }: { icon: keyof typeof Ionicons.glyphMap; label: string; busy: boolean; onPress: () => void }) {
  const theme = useTheme();
  return (
    <Pressable
      onPress={busy ? undefined : onPress}
      style={{
        flex: 1, height: 52, borderRadius: Radius.md, borderWidth: 1, borderColor: theme.border, borderStyle: 'dashed',
        flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6, backgroundColor: theme.surfaceMuted, opacity: busy ? 0.6 : 1,
      }}>
      {busy ? (
        <ActivityIndicator color={theme.primary} />
      ) : (
        <>
          <Ionicons name={icon} size={18} color={theme.textSecondary} />
          <AppText variant="caption" color="textSecondary">{label}</AppText>
        </>
      )}
    </Pressable>
  );
}
