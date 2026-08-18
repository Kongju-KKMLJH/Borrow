package kkmljh.borrow.activity.dto;

import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;

import java.time.Duration;

/** U-11 전송 결과 / U-12 상태 조회 — 매칭 확정 화면의 가격 구성·예상 운영 수익 포함 */
public record HostingRequestResponse(
        Long id,
        Long activityId,
        Long spaceId,
        String spaceName,
        RequestStatus status,
        String rejectReason,
        PriceBreakdown price
) {
    /**
     * 가격 구성과 예상 운영 수익.
     *
     * <p>정식 가격·Pro 요금제·정산 정책은 MVP 범위 밖이다. 매칭 이용료는
     * {@code platform.fee.matching} 설정값(시범 운영 가정값 5,000원)을 그대로 표시한다.
     */
    public record PriceBreakdown(
            int participantPrice,
            int expectedParticipantRevenue,
            int spaceRentalFee,
            int platformMatchingFee,
            int expectedOperatingProfit
    ) {
        static PriceBreakdown of(HostingRequest r, int matchingFee) {
            int participantPrice = r.getActivity().getEntryFee();
            int expectedParticipantRevenue = participantPrice * r.getActivity().getCapacity();

            long minutes = Duration.between(r.getActivity().getStartTime(), r.getActivity().getEndTime()).toMinutes();
            int spaceRentalFee = Math.round(r.getSpace().getHourlyFee() * (minutes / 60f));

            int expectedOperatingProfit = expectedParticipantRevenue - spaceRentalFee - matchingFee;

            return new PriceBreakdown(
                    participantPrice,
                    expectedParticipantRevenue,
                    spaceRentalFee,
                    matchingFee,
                    expectedOperatingProfit
            );
        }
    }

    public static HostingRequestResponse from(HostingRequest r, int matchingFee) {
        return new HostingRequestResponse(
                r.getId(),
                r.getActivity().getId(),
                r.getSpace().getId(),
                r.getSpace().getName(),
                r.getStatus(),
                r.getRejectReason(),
                PriceBreakdown.of(r, matchingFee)
        );
    }
}
