package kkmljh.borrow.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 예술가 인증 게이트 설정 (기능명세 1.2).
 *
 * <p><b>기본값은 자바 필드에서 {@code false} 로 둔다.</b> {@code application.yml} 이 gitignore 라
 * "설정이 없을 때의 동작"이 사실상 기본 동작이다 — {@code platform.fee.matching} 이 기본값 없는
 * {@code int} 라 설정 누락 시 매칭 이용료가 0원으로 표시되는 함정을 반복하지 않는다.
 *
 * <p>기본이 {@code false} 인 이유: 게이트를 켜는 순간 기존 데모 계정이 전부 배지를 잃고,
 * 시연 도중에는 계정을 다시 만들 수 없다.
 *
 * <p>켜더라도 <b>미인증 ARTIST의 활동 개설을 막지는 않는다.</b> 인증 여부가 좌우하는 것은
 * 인증 배지({@code Activity.hostCertified}, F-01)뿐이다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "artist.verification")
public class ArtistVerificationProperties {

    /** true 면 인증이 승인된 ARTIST에게만 배지를 준다. 기본값 false (역할만으로 배지 부여 — 기존 동작). */
    private boolean required = false;
}
