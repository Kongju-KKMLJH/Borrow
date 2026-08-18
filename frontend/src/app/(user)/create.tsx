import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { activityApi, aiApi } from '@/lib/api';
import { ChipGroup, Field, TextField, ToggleRow } from '@/components/form';
import { DialField } from '@/components/date-time-field';
import { ImageUploadField } from '@/components/image-upload-field';
import { ScreenHeader } from '@/components/nav';
import { SpaceMatchCard } from '@/components/space-match-card';
import { AppText, Button, Card } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import type { ActivityField, FacilityType, SpaceMatchResponse } from '@/lib/api/types';
import { ActivityFieldLabel, FacilityTypeLabel, formatCurrency, formatDate, formatTimeRange } from '@/lib/format';
import { useTheme } from '@/hooks/use-theme';

const STEP_LABELS = ['활동 유형', '활동 정보', '공간 조건', 'AI 분석', '공간 추천', '개최 요청'];
const FACILITY_OPTIONS = (Object.keys(FacilityTypeLabel) as FacilityType[]).map((v) => ({ label: FacilityTypeLabel[v], value: v }));

/** step1 — 분야별 세부 활동(하위 카테고리) */
const SUBCATEGORIES: Record<ActivityField, string[]> = {
  ART: ['드로잉', '수채화', '캐릭터 그리기', '기타'],
  PHOTO: ['인물', '풍경', '제품', '기타'],
};
const FIELD_ICON: Record<ActivityField, keyof typeof Ionicons.glyphMap> = { ART: 'people', PHOTO: 'star-outline' };

type Difficulty = '초보 환영' | '경험자' | '제한 없음';
const DIFFICULTIES: Difficulty[] = ['초보 환영', '경험자', '제한 없음'];

export default function Create() {
  const theme = useTheme();
  const [step, setStep] = useState(1);

  // 활동 정보
  const [field, setField] = useState<ActivityField>('ART');
  const [subCategory, setSubCategory] = useState<string>(SUBCATEGORIES.ART[0]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [capacity, setCapacity] = useState('8');
  const [entryFee, setEntryFee] = useState('15000');
  const [difficulty, setDifficulty] = useState<Difficulty>('초보 환영');
  const [preparation, setPreparation] = useState('');
  const [imageUrls, setImageUrls] = useState<string[]>([]);

  const selectField = (f: ActivityField) => {
    setField(f);
    setSubCategory(SUBCATEGORIES[f][0]);
  };

  // 공간 조건
  const [region, setRegion] = useState('천안');
  const [facilities, setFacilities] = useState<FacilityType[]>(['TABLE', 'NATURAL_LIGHT', 'OUTLET']);
  const [noisy, setNoisy] = useState(false);
  const [messy, setMessy] = useState(true);

  const [analyzing, setAnalyzing] = useState(false);
  const [matchLoading, setMatchLoading] = useState(false);
  const [matches, setMatches] = useState<SpaceMatchResponse[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [selected, setSelected] = useState<SpaceMatchResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toggleFacility = (v: string) =>
    setFacilities((arr) => (arr.includes(v as FacilityType) ? arr.filter((x) => x !== v) : [...arr, v as FacilityType]));

  const capacityNum = Number(capacity) || 0;
  const entryFeeNum = Number(entryFee) || 0;
  const step2Valid = title.trim() && /^\d{4}-\d{2}-\d{2}$/.test(date) && /^\d{2}:\d{2}$/.test(startTime) && /^\d{2}:\d{2}$/.test(endTime) && capacityNum > 0;

  const runAnalyze = async () => {
    setError(null);
    setAnalyzing(true);
    try {
      const context = [
        `${title}.`,
        description,
        `세부 활동: ${subCategory}.`,
        `난이도: ${difficulty}.`,
        preparation.trim() && `준비물: ${preparation.trim()}.`,
      ]
        .filter(Boolean)
        .join(' ');
      const result = await aiApi.analyze({ description: context, region: region || null });
      setField(result.field);
      setRegion(result.region || region);
      setFacilities(result.requiredFacilities);
      setNoisy(result.noisy);
      setMessy(result.messy);
      setStep(4);
    } catch {
      setError('AI 분석에 실패했어요. 입력한 조건으로 계속 진행할 수 있어요.');
      setStep(4);
    } finally {
      setAnalyzing(false);
    }
  };

  const goRecommend = async () => {
    setMatchLoading(true);
    try {
      const res = await aiApi.match({
        region, headcount: capacityNum, requiredFacilities: facilities, noisy, messy, field, date, startTime, endTime, excludeSpaceIds: null,
      });
      setMatches(res.matched);
      setSuggestions(res.suggestions);
      setStep(5);
    } finally {
      setMatchLoading(false);
    }
  };

  const selectSpace = (m: SpaceMatchResponse) => { setSelected(m); setStep(6); };

  const submit = async () => {
    if (!selected) return;
    setSubmitting(true);
    try {
      const activity = await activityApi.create({
        field,
        title: title.trim(),
        description: description.trim() || null,
        imageUrls: imageUrls.length > 0 ? imageUrls : null,
        date, startTime, endTime,
        capacity: capacityNum,
        entryFee: entryFeeNum,
        requirement: { region, headcount: capacityNum, requiredFacilities: facilities, noisy, messy },
      });
      await activityApi.sendHostingRequest(activity.id, { spaceId: selected.spaceId });
      setStep(7);
    } catch {
      setError('개최 요청 전송에 실패했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

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
            <Field label="세부 활동">
              <View style={{ gap: Spacing.md }}>
                {(Object.keys(SUBCATEGORIES) as ActivityField[]).map((f) => (
                  <FieldSelectCard
                    key={f}
                    field={f}
                    selected={field === f}
                    subCategory={subCategory}
                    onSelectField={() => selectField(f)}
                    onSelectSub={setSubCategory}
                  />
                ))}
              </View>
            </Field>
          </>
        )}

        {step === 2 && (
          <>
            <Title title="활동 정보를 입력해주세요" />
            <TextField label="활동 제목" value={title} onChangeText={setTitle} placeholder="예) 수채화로 그리는 주말 오후 드로잉" />
            <TextField label="활동 설명" value={description} onChangeText={setDescription} placeholder="어떤 활동인지 자유롭게 소개해주세요" multiline />
            <DialField label="활동 날짜" mode="date" value={date} onChange={setDate} />
            <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
              <View style={{ flex: 1 }}><DialField label="시작 시간" mode="time" value={startTime} onChange={setStartTime} /></View>
              <View style={{ flex: 1 }}><DialField label="종료 시간" mode="time" value={endTime} onChange={setEndTime} /></View>
            </View>
            <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
              <View style={{ flex: 1 }}><TextField label="모집 인원" value={capacity} onChangeText={setCapacity} placeholder="8" keyboardType="numeric" /></View>
              <View style={{ flex: 1 }}><TextField label="참가비" value={entryFee} onChangeText={setEntryFee} placeholder="15000" keyboardType="numeric" /></View>
            </View>
            <ChipGroup label="난이도" options={DIFFICULTIES.map((d) => ({ label: d, value: d }))} selected={[difficulty]} onToggle={(v) => setDifficulty(v as Difficulty)} />
            <TextField label="준비물" value={preparation} onChangeText={setPreparation} placeholder="예) 앞치마, 편한 복장" />
            <ImageUploadField label="활동 사진" value={imageUrls} onChange={setImageUrls} max={5} />
          </>
        )}

        {step === 3 && (
          <>
            <Title title="어떤 공간이 필요한가요?" sub="입력한 조건은 AI 분석 시 기본값으로 쓰이고, 분석 후 자동으로 채워져요." />
            <TextField label="희망 지역" value={region} onChangeText={setRegion} placeholder="천안 동남구" />
            <ChipGroup label="필요한 시설" options={FACILITY_OPTIONS} selected={facilities} onToggle={toggleFacility} />
            <Field label="활동 특성">
              <Card tone="flat" padding="lg" radius="md" style={{ paddingVertical: 0 }}>
                <ToggleRow label="소음 발생 가능" value={noisy} onValueChange={setNoisy} divider />
                <ToggleRow label="오염(물감 등) 가능" value={messy} onValueChange={setMessy} />
              </Card>
            </Field>
            {error && <AppText variant="caption" tint={theme.danger}>{error}</AppText>}
          </>
        )}

        {step === 4 && (
          <>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <Ionicons name="sparkles" size={22} color={theme.primary} />
              <AppText variant="h1">AI 분석 완료</AppText>
            </View>
            <AppText variant="body" color="textSecondary">입력한 활동을 바탕으로 필요한 공간 조건을 추출했어요.</AppText>

            <View style={{ gap: Spacing.sm }}>
              <AppText variant="h3">활동 요약</AppText>
              <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
                <Row label="활동 유형" value={`취미 모임 · ${ActivityFieldLabel[field]}`} />
                <Row label="날짜 · 시간" value={date ? `${formatDate(date)} ${formatTimeRange(startTime, endTime)}` : '-'} />
                <Row label="모집 인원" value={`${capacityNum}명`} last />
              </Card>
            </View>

            <View style={{ gap: Spacing.sm }}>
              <AppText variant="h3">AI 추출 공간 조건</AppText>
              <Card tone="flat" padding="lg" radius="lg" shadow="none" style={{ gap: Spacing.lg, borderWidth: 1.5, borderColor: theme.primary + '40', backgroundColor: theme.primarySoft }}>
                <CondBlock label="희망 지역" value={region || '무관'} />
                <CondBlock label="필요 시설" value={facilities.length ? facilities.map((f) => FacilityTypeLabel[f]).join(', ') : '없음'} />
                <CondBlock label="활동 특성" value={[noisy && '소음 발생', messy && '오염 가능'].filter(Boolean).join(' · ') || '해당 없음'} />
              </Card>
            </View>

            {error && <AppText variant="caption" tint={theme.danger}>{error}</AppText>}
            <AppText variant="caption" color="textMuted">조건이 맞지 않으면 이전 단계에서 직접 수정할 수 있어요.</AppText>
          </>
        )}

        {step === 5 && (
          <>
            <Title title="이런 공간을 추천해요" sub={matches.length > 0 ? `조건에 맞는 유휴 공간 ${matches.length}곳을 찾았어요.` : '조건에 맞는 공간을 찾지 못했어요.'} />
            {matches.map((m) => (
              <SpaceMatchCard key={m.spaceId} match={m} onSelect={() => selectSpace(m)} />
            ))}
            {matches.length === 0 && !matchLoading && suggestions.length > 0 && (
              <View style={{ gap: Spacing.sm, paddingHorizontal: Spacing.md }}>
                {suggestions.map((s, i) => (
                  <View key={i} style={{ flexDirection: 'row', gap: Spacing.sm, alignItems: 'flex-start' }}>
                    <AppText variant="body" tint={theme.primary}>•</AppText>
                    <AppText variant="body" color="textSecondary" style={{ flex: 1 }}>{s}</AppText>
                  </View>
                ))}
              </View>
            )}
          </>
        )}

        {step === 6 && selected && (
          <>
            <Title title="개최 요청을 확인해주세요" sub="공간 제공자에게 아래 내용으로 요청이 전달돼요." />

            <View style={{ gap: Spacing.sm }}>
              <AppText variant="h3">활동 정보</AppText>
              <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
                <Row label="활동명" value={title} />
                <Row label="유형 · 분야" value={`취미 모임 · ${ActivityFieldLabel[field]}`} />
                <Row label="날짜 · 시간" value={`${formatDate(date)} ${formatTimeRange(startTime, endTime)}`} />
                <Row label="모집 인원" value={`${capacityNum}명`} />
                <Row label="참가비" value={formatCurrency(entryFeeNum)} last />
              </Card>
            </View>

            <View style={{ gap: Spacing.sm }}>
              <AppText variant="h3">선택한 공간</AppText>
              <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.md }}>
                <Row label="공간" value={selected.name} />
                <Row label="지역" value={selected.region} />
                <Row label="시간당 이용료" value={formatCurrency(selected.hourlyFee)} tint={theme.primary} last />
              </Card>
            </View>

            {description ? (
              <View style={{ gap: Spacing.sm }}>
                <AppText variant="h3">활동 설명</AppText>
                <Card tone="muted" padding="lg" radius="lg" shadow="none">
                  <AppText variant="body" color="textSecondary">{description}</AppText>
                </Card>
              </View>
            ) : null}

            {error && <AppText variant="caption" tint={theme.danger}>{error}</AppText>}
          </>
        )}
      </ScrollView>

      {/* 하단 액션바 */}
      <View style={{ flexDirection: 'row', gap: Spacing.sm, padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
        {step > 1 && step !== 5 && <Button label="이전" variant="outline" onPress={() => setStep(step - 1)} style={{ flex: 1 }} />}
        {step === 1 && <Button label="다음" fullWidth onPress={() => setStep(2)} />}
        {step === 2 && <Button label="다음" disabled={!step2Valid} onPress={() => setStep(3)} style={{ flex: 2 }} />}
        {step === 3 && <Button label={analyzing ? '' : 'AI 분석하기'} loading={analyzing} icon="sparkles" onPress={runAnalyze} style={{ flex: 2 }} />}
        {step === 4 && <Button label={matchLoading ? '' : '추천 공간 보기'} loading={matchLoading} onPress={goRecommend} style={{ flex: 2 }} />}
        {step === 5 && <Button label="조건 다시 설정" variant="outline" fullWidth onPress={() => setStep(3)} />}
        {step === 6 && <Button label={submitting ? '' : '개최 요청 보내기'} loading={submitting} onPress={submit} style={{ flex: 2 }} />}
      </View>
    </SafeAreaView>
  );
}

function Stepper({ current, total, label }: { current: number; total: number; label: string }) {
  const theme = useTheme();
  return (
    <View style={{ gap: Spacing.sm }}>
      <AppText variant="small" tint={theme.primary}>STEP {current} / {total} · {label}</AppText>
      <View style={{ flexDirection: 'row', gap: 4 }}>
        {Array.from({ length: total }).map((_, i) => (
          <View key={i} style={{ flex: 1, height: 4, borderRadius: Radius.full, backgroundColor: i < current ? theme.primary : theme.surfaceDeep }} />
        ))}
      </View>
    </View>
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

function Row({ label, value, last, tint }: { label: string; value: string; last?: boolean; tint?: string }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.xs, borderBottomWidth: last ? 0 : 1, borderBottomColor: theme.border, paddingBottom: last ? 0 : Spacing.md }}>
      <AppText variant="body" color="textMuted">{label}</AppText>
      <AppText variant="title" tint={tint} style={{ flex: 1, textAlign: 'right' }} numberOfLines={1}>{value}</AppText>
    </View>
  );
}

/** AI 추출 공간 조건 블록 — 코랄 라벨 + 다크 값 (세로 배치) */
function CondBlock({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ gap: 3 }}>
      <AppText variant="label" color="primary">{label}</AppText>
      <AppText variant="body">{value}</AppText>
    </View>
  );
}

/** step1 — 분야 선택 카드. 선택하면 세부 활동 하위 칩이 펼쳐진다. */
function FieldSelectCard({
  field,
  selected,
  subCategory,
  onSelectField,
  onSelectSub,
}: {
  field: ActivityField;
  selected: boolean;
  subCategory: string;
  onSelectField: () => void;
  onSelectSub: (value: string) => void;
}) {
  const theme = useTheme();
  return (
    <Card
      onPress={onSelectField}
      tone={selected ? 'surface' : 'flat'}
      padding="lg"
      radius="lg"
      shadow="none"
      style={{ gap: selected ? Spacing.lg : 0, borderWidth: 2, borderColor: selected ? theme.primary : theme.border, backgroundColor: selected ? theme.primarySoft : theme.surface }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
        <Ionicons name={FIELD_ICON[field]} size={22} color={selected ? theme.primary : theme.textMuted} />
        <AppText variant="h3" color={selected ? 'text' : 'textSecondary'}>{ActivityFieldLabel[field]}</AppText>
      </View>
      {selected && (
        <ChipGroup
          options={SUBCATEGORIES[field].map((s) => ({ label: s, value: s }))}
          selected={[subCategory]}
          onToggle={onSelectSub}
        />
      )}
    </Card>
  );
}
