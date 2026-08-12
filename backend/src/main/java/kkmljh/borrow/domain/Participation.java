package kkmljh.borrow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 활동 참여 신청 (U-04 신청, U-05 취소) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "guestId"}))
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    /** 참여자 로그인 아이디 — 취소/내 활동 조회에 사용 */
    @Column(nullable = false)
    private String guestId;

    @Column(nullable = false)
    private String nickname;

    /** 참여 인원 수 */
    @Column(nullable = false)
    private int headcount;

    @Builder
    private Participation(Activity activity, String guestId, String nickname, int headcount) {
        this.activity = activity;
        this.guestId = guestId;
        this.nickname = nickname;
        this.headcount = headcount;
    }
}
