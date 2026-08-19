package kkmljh.borrow.activity.repository;

/**
 * 확정(개최 요청 APPROVED)된 개최지 공간의 요약 — 리포지토리 조회 결과 투영.
 *
 * <p>주소(`Space.address`)를 <b>담지 않는다</b>. 공개 응답의 주소는 동 단위({@code region})까지라는
 * 기능명세 6.1 {@code rules} 정책이 여기서 뚫리면 안 된다. 주소 전문은 소유 HOST 본인 전용
 * 경로({@code SpaceResponse.forOwner})만 준다.
 *
 * @param activityId 이 공간이 확정된 활동 id (목록 배치 조회 결과를 활동별로 나누는 키)
 */
public record ConfirmedSpace(Long activityId, Long spaceId, String name, String region) {
}
