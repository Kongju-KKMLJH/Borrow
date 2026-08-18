import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Dimensions, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { activityApi, imageUri, ApiException } from '@/lib/api';
import { ImagePlaceholder } from '@/components/placeholder';
import { ScreenHeader } from '@/components/nav';
import { ActivityStatusBadge } from '@/components/status-badge';
import { VerifiedBadge } from '@/components/verified-badge';
import { AppText, Avatar, Badge, Button, Card } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { ActivityTypeLabel, ActivityFieldLabel, FacilityTypeLabel, formatCurrency, formatDate, formatTimeRange } from '@/lib/format';
import { useAsync } from '@/hooks/use-async';
import { useTheme } from '@/hooks/use-theme';

export default function ActivityDetail() {
  const theme = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const activityId = Number(id);
  const { data: activity, loading } = useAsync(() => activityApi.detail(activityId), [activityId]);
  const { data: hostingRequest } = useAsync(async () => {
    if (!activity?.mine) return null;
    try {
      return await activityApi.hostingRequestStatus(activityId);
    } catch (e) {
      if (e instanceof ApiException) return null;
      throw e;
    }
  }, [activityId, activity?.mine]);

  const [joinOpen, setJoinOpen] = useState(false);
  const [headcount, setHeadcount] = useState(1);
  const [justJoined, setJustJoined] = useState(false);
  const [joining, setJoining] = useState(false);

  if (loading || !activity) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }}>
        <ScreenHeader title="활동 상세" />
      </SafeAreaView>
    );
  }

  const alreadyJoined = activity.alreadyJoined || justJoined;
  const canJoin = activity.status === 'PUBLISHED' && !activity.mine && !alreadyJoined;

  const submitJoin = async () => {
    setJoining(true);
    try {
      await activityApi.participate(activityId, { headcount });
      setJustJoined(true);
      setJoinOpen(false);
    } finally {
      setJoining(false);
    }
  };

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.background }} edges={['top']}>
      <ScreenHeader title="활동 상세" />
      <ScrollView contentContainerStyle={{ paddingBottom: Spacing.xxxl }} showsVerticalScrollIndicator={false}>
        {/* 히어로 */}
        <View style={{ height: 220 }}>
          {activity.imageUrls && activity.imageUrls.length > 0 ? (
            <ImageGallery urls={activity.imageUrls} />
          ) : (
            <ImagePlaceholder field={activity.field} height={220} radius="sm" style={{ borderRadius: 0, marginHorizontal: 0 }} />
          )}
          <View style={{ position: 'absolute', top: Spacing.lg, left: Spacing.lg, flexDirection: 'row', gap: 6 }}>
            <Badge label={ActivityTypeLabel[activity.type]} tone="solid" />
            <Badge label={ActivityFieldLabel[activity.field]} tone="primary" />
          </View>
          <View style={{ position: 'absolute', top: Spacing.lg, right: Spacing.lg }}>
            <ActivityStatusBadge status={activity.status} />
          </View>
        </View>

        <View style={{ padding: Spacing.xl, gap: Spacing.xl }}>
          {/* 소개 */}
          <View style={{ gap: Spacing.sm }}>
            <AppText variant="h1">{activity.title}</AppText>
            {activity.description ? <AppText variant="body" color="textSecondary">{activity.description}</AppText> : null}
          </View>

          {/* 진행자 */}
          <Card tone="muted" padding="lg" radius="lg" shadow="none" style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
            <Avatar name={activity.hostNickname ?? undefined} size={44} />
            <View style={{ flex: 1, gap: 3 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                <AppText variant="title">{activity.hostNickname}</AppText>
                {activity.hostCertified && <VerifiedBadge size={17} />}
              </View>
              <AppText variant="caption" color="textMuted">진행자</AppText>
            </View>
          </Card>

          {/* 활동 정보 */}
          <Section title="활동 정보">
            <Row label="활동 날짜" value={formatDate(activity.date)} />
            <Row label="활동 시간" value={formatTimeRange(activity.startTime, activity.endTime)} />
            <Row label="현재 인원" value={`${activity.currentHeadcount} / ${activity.capacity}명`} />
            {activity.status === 'PUBLISHED' && (
              <Row label="잔여 인원" value={`${activity.remainingCapacity}석`} last />
            )}
          </Section>

          {/* 공간 조건 */}
          {activity.requirement && (
            <Section title="희망 공간 조건">
              <Row label="희망 지역" value={activity.requirement.region || '무관'} />
              <Row label="필요 인원" value={`${activity.requirement.headcount}명`} />
              <Row
                label="필요 시설"
                value={activity.requirement.requiredFacilities?.length ? activity.requirement.requiredFacilities.map((f) => FacilityTypeLabel[f]).join(', ') : '없음'}
              />
              <Row
                label="활동 특성"
                value={[activity.requirement.noisy && '소음 발생', activity.requirement.messy && '오염 가능'].filter(Boolean).join(' · ') || '없음'}
                last
              />
            </Section>
          )}

          {/* 확정된 공간 (activity.space — 개최 요청이 APPROVED일 때만 내려옴) */}
          {activity.space && (
            <Section title="확정된 공간">
              <Row label="공간명" value={activity.space.name} />
              <Row label="지역" value={activity.space.region} last />
            </Section>
          )}

          {/* 참가비 */}
          <Section title="참가비">
            <Row label="1인 참가비" value={formatCurrency(activity.entryFee)} last />
          </Section>
        </View>
      </ScrollView>

      {/* 참여 신청 폼 */}
      {joinOpen && (
        <View style={{ padding: Spacing.xl, paddingTop: Spacing.md, gap: Spacing.sm, borderTopWidth: 1, borderTopColor: theme.border }}>
          <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
            <AppText variant="body" color="textMuted">참여 인원</AppText>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
              <Ionicons name="remove-circle-outline" size={26} color={theme.text} onPress={() => setHeadcount((h) => Math.max(1, h - 1))} />
              <AppText variant="title">{headcount}명</AppText>
              <Ionicons name="add-circle-outline" size={26} color={theme.text} onPress={() => setHeadcount((h) => h + 1)} />
            </View>
          </View>
          <Button label={joining ? '' : '신청 완료'} loading={joining} fullWidth onPress={submitJoin} />
        </View>
      )}

      {/* 하단 CTA */}
      {!joinOpen && (
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.lg, padding: Spacing.xl, paddingTop: Spacing.md, borderTopWidth: 1, borderTopColor: theme.border }}>
          <View>
            <AppText variant="caption" color="textMuted">1인 참가비</AppText>
            <AppText variant="h3">{formatCurrency(activity.entryFee)}</AppText>
          </View>
          {activity.mine ? (
            <Button label="내가 개설한 활동" disabled fullWidth style={{ flex: 1 }} />
          ) : alreadyJoined ? (
            <Button label="참여 신청 완료" disabled style={{ flex: 1 }} />
          ) : (
            <Button label="참여 신청하기" disabled={!canJoin} onPress={() => setJoinOpen(true)} style={{ flex: 1 }} />
          )}
        </View>
      )}
    </SafeAreaView>
  );
}

/** 활동 이미지 가로 스와이프 갤러리 + 페이지 인디케이터. */
function ImageGallery({ urls }: { urls: string[] }) {
  const [page, setPage] = useState(0);
  const width = Dimensions.get('window').width;
  return (
    <View style={{ width, height: 220 }}>
      <ScrollView
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onMomentumScrollEnd={(e) => setPage(Math.round(e.nativeEvent.contentOffset.x / width))}>
        {urls.map((u, i) => (
          <Image key={`${u}-${i}`} source={{ uri: imageUri(u) }} style={{ width, height: 220 }} contentFit="cover" transition={150} />
        ))}
      </ScrollView>
      {urls.length > 1 && (
        <View style={{ position: 'absolute', bottom: Spacing.md, alignSelf: 'center', flexDirection: 'row', gap: 5 }}>
          {urls.map((_, i) => (
            <View
              key={i}
              style={{ width: i === page ? 16 : 6, height: 6, borderRadius: 3, backgroundColor: i === page ? '#FFFFFF' : 'rgba(255,255,255,0.5)' }}
            />
          ))}
        </View>
      )}
    </View>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={{ gap: Spacing.sm }}>
      <AppText variant="h3">{title}</AppText>
      <Card tone="flat" padding="lg" radius="lg" style={{ gap: 0 }}>{children}</Card>
    </View>
  );
}

function Row({ label, value, last }: { label: string; value: string; last?: boolean }) {
  const theme = useTheme();
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: Spacing.md, borderBottomWidth: last ? 0 : 1, borderBottomColor: theme.border }}>
      <AppText variant="body" color="textMuted">{label}</AppText>
      <AppText variant="title" style={{ flex: 1, textAlign: 'right' }} numberOfLines={2}>{value}</AppText>
    </View>
  );
}
