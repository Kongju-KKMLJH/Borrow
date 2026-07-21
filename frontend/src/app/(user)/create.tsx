import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { activitiesApi, spacesApi } from '@/api';
import { ChipGroup, Field, SelectField, Stepper, TextField, ToggleRow } from '@/components/form';
import { ScreenHeader } from '@/components/nav';
import { SpaceMatchCard } from '@/components/space-match-card';
import { AppText, Button, Card } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import type { SpaceMatch, Space } from '@/data/types';
import { useTheme } from '@/hooks/use-theme';

const STEP_LABELS = ['활동 유형', '활동 정보', '공간 조건', 'AI 분석', '공간 추천', '개최 요청'];
const FACILITIES = ['넓은 테이블', '의자', '자연광', '촬영 조명', '촬영 배경', '물 사용', '세면시설', '콘센트'];

export default function Create() {
  const theme = useTheme();
  const [step, setStep] = useState(1);
  const [type, setType] = useState<'hobby' | 'class'>('hobby');
  const [field, setField] = useState<'drawing' | 'photo'>('drawing');
  const [detail, setDetail] = useState('드로잉');
  const [difficulty, setDifficulty] = useState('초보 환영');
  const [facilities, setFacilities] = useState<string[]>(['넓은 테이블', '자연광', '물 사용', '콘센트']);
  const [mess, setMess] = useState(true);
  const [noise, setNoise] = useState(false);
  const [equip, setEquip] = useState(false);
  const [matches, setMatches] = useState<SpaceMatch[]>([]);
  const [space, setSpace] = useState<Space | null>(null);
  const [loading, setLoading] = useState(false);

  const toggle = (arr: string[], v: string, set: (a: string[]) => void) =>
    set(arr.includes(v) ? arr.filter((x) => x !== v) : [...arr, v]);

  const goRecommend = async () => {
    setLoading(true);
    const res = await spacesApi.recommendSpaces({ facilities });
    setMatches(res);
    setLoading(false);
    setStep(5);
  };
  const selectSpace = (s: Space) => { setSpace(s); setStep(6); };
  const submit = async () => { await activitiesApi.createActivity({ title: '새 활동', space: space ?? undefined }); setStep(7); };

  // ── Step 7: 완료 ──
  if (step === 7) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }}>
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', padding: Spacing.xxxl, gap: Spacing.lg }}>
          <View style={{ width: 88, height: 88, borderRadius: 44, backgroundColor: theme.primarySoft, alignItems: 'center', justifyContent: 'center' }}>
            <Ionicons name="checkmark" size={44} color={theme.primary} />
          </View>
          <AppText variant="h1" center>개최 요청을 보냈어요</AppText>
          <AppText variant="body" color="textSecondary" center>공간 제공자가 확인 후 승인하면{'\n'}활동 모집이 시작돼요.</AppText>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 5, backgroundColor: theme.accentSoft, paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm, borderRadius: Radius.full }}>
            <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: theme.accent }} />
            <AppText variant="tiny" tint={theme.accentPressed}>공간 승인 대기 중</AppText>
          </View>
        </View>
        <View style={{ padding: Spacing.xl, gap: Spacing.sm }}>
          <Button label="내 활동에서 확인" fullWidth onPress={() => router.replace('/(user)/my-activities')} />
          <Button label="홈으로 이동" variant="outline" fullWidth onPress={() => router.replace('/(user)')} />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="활동 만들기" onBack={() => (step > 1 ? setStep(step - 1) : router.back())} />
      <View style={{ paddingHorizontal: Spacing.xl, paddingBottom: Spacing.md }}>
        <Stepper current={step} total={6} label={STEP_LABELS[step - 1]} />
      </View>

      <ScrollView contentContainerStyle={{ padding: Spacing.xl, paddingTop: Spacing.sm, gap: Spacing.xl }} showsVerticalScrollIndicator={false}>
        {step === 1 && (
          <>
            <Title title="어떤 활동을 만들까요?" sub="활동 유형을 선택해주세요." />
            <TypeCard icon="people-outline" title="취미 모임 만들기" desc="강사 없이 관심사가 비슷한 사람들과 함께 즐겨요. 누구나 개설할 수 있어요." selected={type === 'hobby'} onPress={() => setType('hobby')} />
            <TypeCard icon="star-outline" title="전문 클래스 만들기" desc="인증 예술가만 개설할 수 있어요. (해커톤 준비 중)" locked />
          </>
        )}

        {step === 2 && (
          <>
            <Title title="활동 정보를 입력해주세요" />
            <ChipGroup label="활동 분야" options={[{ label: '그림', value: 'drawing' }, { label: '촬영', value: 'photo' }]} selected={[field]} onToggle={(v) => setField(v as any)} />
            <ChipGroup label="세부 활동" options={['드로잉', '수채화', '캐릭터 그리기', '사진 산책', '인물 촬영', '기타'].map((v) => ({ label: v, value: v }))} selected={[detail]} onToggle={setDetail} />
            <TextField label="활동 제목" placeholder="예) 수채화로 그리는 주말 오후 드로잉" />
            <TextField label="활동 설명" placeholder="어떤 활동인지 자유롭게 소개해주세요" multiline />
            <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
              <View style={{ flex: 1 }}><TextField label="활동 날짜" placeholder="10월 24일" /></View>
              <View style={{ flex: 1 }}><TextField label="활동 시간" placeholder="14:00" /></View>
            </View>
            <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
              <View style={{ flex: 1 }}><TextField label="모집 인원" placeholder="8명" keyboardType="numeric" /></View>
              <View style={{ flex: 1 }}><TextField label="참가비" placeholder="15,000원" keyboardType="numeric" /></View>
            </View>
            <ChipGroup label="난이도" options={['초보 환영', '경험자', '제한 없음'].map((v) => ({ label: v, value: v }))} selected={[difficulty]} onToggle={setDifficulty} />
          </>
        )}

        {step === 3 && (
          <>
            <Title title="어떤 공간이 필요한가요?" sub="AI가 조건에 맞는 유휴 공간을 추천해드려요." />
            <SelectField label="희망 지역" value="천안 동남구" />
            <SelectField label="활동 인원" value="8명 (자동 입력됨)" />
            <ChipGroup label="필요한 시설" options={FACILITIES.map((v) => ({ label: v, value: v }))} selected={facilities} onToggle={(v) => toggle(facilities, v, setFacilities)} />
            <Field label="활동 특성">
              <Card tone="flat" padding="lg" radius="md" style={{ paddingVertical: 0 }}>
                <ToggleRow label="소음 발생 가능" value={noise} onValueChange={setNoise} divider />
                <ToggleRow label="오염(물감 등) 가능" value={mess} onValueChange={setMess} divider />
                <ToggleRow label="장비 사용" value={equip} onValueChange={setEquip} />
              </Card>
            </Field>
            <TextField label="기타 요청사항" placeholder="공간에 바라는 점을 자유롭게 적어주세요" multiline />
          </>
        )}

        {step === 4 && (
          <>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <Ionicons name="sparkles" size={22} color={theme.primary} />
              <AppText variant="h1">AI 분석 완료</AppText>
            </View>
            <AppText variant="body" color="textSecondary">입력한 활동을 바탕으로 필요한 공간 조건을 추출했어요.</AppText>
            <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
              <Row label="필요한 공간 규모" value="중형 (8~12인)" />
              <Row label="필요 시설" value="테이블·자연광·물·콘센트" />
              <Row label="허용 활동" value="물감·수채 사용" />
              <Row label="추천 공간 유형" value="카페 · 갤러리" last />
            </Card>
            <AppText variant="caption" color="textMuted">조건이 맞지 않으면 이전 단계에서 수정할 수 있어요.</AppText>
          </>
        )}

        {step === 5 && (
          <>
            <Title title="이런 공간을 추천해요" sub={`조건에 맞는 유휴 공간 ${matches.length}곳을 찾았어요.`} />
            {matches.map((m) => (
              <SpaceMatchCard key={m.space.id} match={m} onSelect={() => selectSpace(m.space)} />
            ))}
          </>
        )}

        {step === 6 && (
          <>
            <Title title="개최 요청을 확인해주세요" sub="공간 제공자에게 아래 내용으로 요청이 전달돼요." />
            <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
              <Row label="활동명" value="수채화로 그리는 주말 오후 드로잉" />
              <Row label="유형 · 분야" value="취미 모임 · 그림" />
              <Row label="날짜 · 시간" value="10월 24일 오후 2:00~4:00" />
              <Row label="모집 인원" value="8명" />
              <Row label="참가비" value="15,000원" last />
            </Card>
            <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
              <Row label="선택 공간" value={space?.name ?? '-'} />
              <Row label="위치" value={space?.address ?? '-'} />
              <Row label="이용 조건" value={space?.notes ?? '-'} last />
            </Card>
          </>
        )}
      </ScrollView>

      {/* 하단 액션바 */}
      <View style={{ flexDirection: 'row', gap: Spacing.sm, padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        {step > 1 && step !== 5 && <Button label="이전" variant="outline" onPress={() => setStep(step - 1)} style={{ flex: 1 }} />}
        {step === 1 && <Button label="다음" fullWidth onPress={() => setStep(2)} />}
        {step === 2 && <Button label="다음" onPress={() => setStep(3)} style={{ flex: 2 }} />}
        {step === 3 && <Button label="AI 분석하기" icon="sparkles" onPress={() => setStep(4)} style={{ flex: 2 }} />}
        {step === 4 && <Button label={loading ? '' : '추천 공간 보기'} loading={loading} onPress={goRecommend} style={{ flex: 2 }} />}
        {step === 5 && <Button label="조건 다시 설정" variant="outline" fullWidth onPress={() => setStep(3)} />}
        {step === 6 && <Button label="개최 요청 보내기" onPress={submit} style={{ flex: 2 }} />}
      </View>
    </SafeAreaView>
  );
}

function Title({ title, sub }: { title: string; sub?: string }) {
  return (
    <View style={{ gap: Spacing.sm }}>
      <AppText variant="h1">{title}</AppText>
      {sub ? <AppText variant="body" color="textSecondary">{sub}</AppText> : null}
    </View>
  );
}

function Row({ label, value, last }: { label: string; value: string; last?: boolean }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.xs, borderBottomWidth: last ? 0 : 1, borderBottomColor: theme.border, paddingBottom: last ? 0 : Spacing.md }}>
      <AppText variant="body" color="textMuted">{label}</AppText>
      <AppText variant="title" style={{ flex: 1, textAlign: 'right' }} numberOfLines={1}>{value}</AppText>
    </View>
  );
}

function TypeCard({ icon, title, desc, selected, locked, onPress }: { icon: keyof typeof Ionicons.glyphMap; title: string; desc: string; selected?: boolean; locked?: boolean; onPress?: () => void }) {
  const theme = useTheme();
  return (
    <Card onPress={locked ? undefined : onPress} tone={selected ? 'surface' : 'flat'} padding="xl" radius="lg" style={{ gap: Spacing.sm, borderWidth: 2, borderColor: selected ? theme.primary : theme.border, backgroundColor: selected ? theme.primarySoft : theme.surface, opacity: locked ? 0.6 : 1 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
        <Ionicons name={icon} size={24} color={locked ? theme.textMuted : theme.primary} />
        <AppText variant="h3" style={{ flex: 1 }} color={locked ? 'textMuted' : 'text'}>{title}</AppText>
        {locked && <Ionicons name="lock-closed" size={18} color={theme.textMuted} />}
      </View>
      <AppText variant="caption" color="textMuted">{desc}</AppText>
    </Card>
  );
}
