import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { meApi } from '@/lib/api';
import { TextField } from '@/components/form';
import { ScreenHeader } from '@/components/nav';
import { AppText, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

const STATUS_LABEL = { NONE: '미신청', PENDING: '심사 중', APPROVED: '승인됨', REJECTED: '반려됨' } as const;

export default function ArtistVerification() {
  const theme = useTheme();
  const { data: verification, loading, refetch } = useAsync(() => meApi.artistVerificationStatus(), []);
  const [portfolioUrl, setPortfolioUrl] = useState('');
  const [career, setCareer] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const apply = async () => {
    if (!portfolioUrl.trim()) { setError('포트폴리오 URL을 입력해주세요.'); return; }
    setSubmitting(true); setError(null);
    try {
      await meApi.applyArtistVerification({ portfolioUrl: portfolioUrl.trim(), career: career.trim() || null });
      await refetch();
    } catch { setError('인증 신청에 실패했어요. 신청 상태를 확인해주세요.'); }
    finally { setSubmitting(false); }
  };

  const canApply = verification?.status === 'NONE' || verification?.status === 'REJECTED';
  return <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
    <ScreenHeader title="예술가 인증" onBack={() => router.back()} />
    <ScrollView contentContainerStyle={{ padding: Spacing.xl, gap: Spacing.lg }}>
      <View style={{ gap: Spacing.sm }}>
        <AppText variant="h1">예술가 인증 신청</AppText>
        <AppText variant="body" color="textSecondary">포트폴리오와 활동 경력을 제출하면 예술가 프로그램 개설 자격을 확인할 수 있어요.</AppText>
      </View>
      {loading ? null : verification && <Card tone="flat" padding="lg" radius="lg" style={{ gap: Spacing.sm }}>
        <AppText variant="title">현재 상태: {STATUS_LABEL[verification.status]}</AppText>
        {verification.reason && <AppText variant="caption" color="textSecondary">반려 사유: {verification.reason}</AppText>}
      </Card>}
      {canApply && <>
        <TextField label="포트폴리오 URL" value={portfolioUrl || verification?.portfolioUrl || ''} onChangeText={setPortfolioUrl} placeholder="https://..." />
        <TextField label="활동 경력" value={career || verification?.career || ''} onChangeText={setCareer} placeholder="작품 활동, 수상 경력 등을 입력해주세요" multiline />
        {error && <AppText variant="caption" tint={theme.danger}>{error}</AppText>}
        <Button label="인증 신청하기" loading={submitting} fullWidth onPress={apply} />
      </>}
      {!canApply && verification?.status === 'APPROVED' && <AppText variant="body" color="textSecondary">인증이 완료된 계정이에요.</AppText>}
      {!canApply && verification?.status === 'PENDING' && <AppText variant="body" color="textSecondary">심사 결과를 기다려주세요.</AppText>}
    </ScrollView>
  </SafeAreaView>;
}
