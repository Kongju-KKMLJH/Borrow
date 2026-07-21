import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { spacesApi } from '@/api';
import { ChipGroup, Field, SelectChip, TextField, ToggleRow } from '@/components/form';
import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Card } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const FACILITIES = ['넓은 테이블', '의자', '자연광', '촬영 조명', '촬영 배경', '세면시설', '콘센트'];
const DAYS = ['월', '화', '수', '목', '금', '토', '일'];

export default function ProviderSpace() {
  const theme = useTheme();
  const [facilities, setFacilities] = useState(['넓은 테이블', '의자', '자연광', '세면시설', '콘센트']);
  const [days, setDays] = useState(['월', '화', '수', '목']);
  const [allow, setAllow] = useState({ drawing: true, photo: true, paint: true, equip: false, noise: true });

  const toggleArr = (arr: string[], v: string, set: (a: string[]) => void) =>
    set(arr.includes(v) ? arr.filter((x) => x !== v) : [...arr, v]);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="공간 관리" />
      <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.xxl, paddingBottom: Spacing.huge }} showsVerticalScrollIndicator={false}>
        {/* 기본 정보 */}
        <Group title="기본 정보">
          <Pressable style={{ borderWidth: 1.5, borderColor: theme.border, borderStyle: 'dashed', borderRadius: Radius.md, paddingVertical: 26, alignItems: 'center', flexDirection: 'row', justifyContent: 'center', gap: 6, backgroundColor: theme.surfaceMuted }}>
            <Ionicons name="add" size={20} color={theme.textMuted} />
            <AppText variant="body" color="textMuted">공간 사진 추가</AppText>
          </Pressable>
          <TextField label="공간명" value="브루 랩 카페" />
          <TextField label="공간 주소" value="천안 서북구 두정동 100" />
          <TextField label="공간 소개" placeholder="공간을 자유롭게 소개해주세요" multiline />
          <TextField label="최대 수용 인원" value="12명" keyboardType="numeric" />
        </Group>

        {/* 제공 시설 */}
        <Group title="제공 시설">
          <ChipGroup options={FACILITIES.map((v) => ({ label: v, value: v }))} selected={facilities} onToggle={(v) => toggleArr(facilities, v, setFacilities)} />
        </Group>

        {/* 허용 활동 */}
        <Group title="허용 활동">
          <Card tone="flat" padding="lg" radius="md" style={{ paddingVertical: 0 }}>
            <ToggleRow label="그림 활동 허용" value={allow.drawing} onValueChange={(v) => setAllow({ ...allow, drawing: v })} divider />
            <ToggleRow label="촬영 활동 허용" value={allow.photo} onValueChange={(v) => setAllow({ ...allow, photo: v })} divider />
            <ToggleRow label="물감 사용 허용" value={allow.paint} onValueChange={(v) => setAllow({ ...allow, paint: v })} divider />
            <ToggleRow label="장비 사용 허용" value={allow.equip} onValueChange={(v) => setAllow({ ...allow, equip: v })} divider />
            <ToggleRow label="소음 활동 제한" value={allow.noise} onValueChange={(v) => setAllow({ ...allow, noise: v })} />
          </Card>
        </Group>

        {/* 제공 가능 시간 */}
        <Group title="제공 가능 시간">
          <Field label="제공 요일">
            <View style={{ flexDirection: 'row', gap: 6 }}>
              {DAYS.map((d) => (
                <View key={d} style={{ flex: 1 }}>
                  <DayChip label={d} active={days.includes(d)} onPress={() => toggleArr(days, d, setDays)} />
                </View>
              ))}
            </View>
          </Field>
          <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
            <View style={{ flex: 1 }}><TextField label="시작 시간" value="14:00" /></View>
            <View style={{ flex: 1 }}><TextField label="종료 시간" value="17:00" /></View>
          </View>
          <Pressable style={{ borderWidth: 1.5, borderColor: theme.border, borderStyle: 'dashed', borderRadius: Radius.md, paddingVertical: 13, alignItems: 'center', flexDirection: 'row', justifyContent: 'center', gap: 6, backgroundColor: theme.surfaceMuted }}>
            <Ionicons name="add" size={18} color={theme.textMuted} />
            <AppText variant="body" color="textMuted">시간대 추가 (학기 중 / 방학 중)</AppText>
          </Pressable>
        </Group>

        {/* 이용 조건 */}
        <Group title="이용 조건">
          <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
            <View style={{ flex: 1 }}><TextField label="공간 이용료" value="30,000원" keyboardType="numeric" /></View>
            <View style={{ flex: 1 }}><TextField label="최소 이용 시간" value="2시간" /></View>
          </View>
          <TextField label="음료 주문 조건" value="인당 음료 1잔 주문 필수" />
          <TextField label="기타 주의사항" placeholder="이용 시 안내할 내용을 적어주세요" multiline />
        </Group>
      </ScrollView>

      <View style={{ padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        <Button label="공간 정보 저장" fullWidth onPress={async () => { await spacesApi.saveSpace({ facilities }); router.back(); }} />
      </View>
    </SafeAreaView>
  );
}

function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={{ gap: Spacing.md }}>
      <AppText variant="h3">{title}</AppText>
      {children}
    </View>
  );
}

function DayChip({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  const theme = useTheme();
  return (
    <Pressable onPress={onPress} style={{ alignItems: 'center', paddingVertical: 10, borderRadius: Radius.md, backgroundColor: active ? theme.text : theme.surfaceMuted, borderWidth: 1, borderColor: active ? theme.text : theme.border }}>
      <AppText variant="small" tint={active ? theme.textInverse : theme.textSecondary}>{label}</AppText>
    </Pressable>
  );
}
