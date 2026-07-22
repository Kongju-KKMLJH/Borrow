package kkmljh.borrow.ai.repository;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * A-02 하드 필터용 공간 조회. Space 엔티티는 B 소유지만 읽기 전용 조회이므로
 * AI 매칭 전용 리포지토리를 별도로 둔다(B의 SpaceRepository와 빈 이름 충돌 회피).
 *
 * <p>DB에서 거르는 하드 조건: 수용인원 / 지역(부분 일치, 양방향) / 허용 분야.
 * 슬롯 시간 겹침과 소음·오염 제한은 서비스에서 자바로 필터링한다.
 */
public interface SpaceMatchRepository extends JpaRepository<Space, Long> {

    @Query("""
            select distinct s from Space s
            where s.capacity >= :headcount
              and (
                :region = ''
                or s.region like concat('%', :region, '%')
                or :region like concat('%', s.region, '%')
              )
              and :field member of s.allowedFields
            """)
    List<Space> findCandidates(@Param("headcount") int headcount,
                               @Param("region") String region,
                               @Param("field") ActivityField field);
}