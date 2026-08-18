package kkmljh.borrow.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 플랫폼 가격 정책 설정값 (시범 운영).
 *
 * <p>매칭 이용료는 화면에 하드코딩하지 않고 {@code platform.fee.matching} 설정으로 분리한다.
 * 실제 지불 의사 검증 후 값을 조정할 수 있도록 {@code platform.fee.policy-editable}로
 * 정책 변경 가능 여부를 함께 관리한다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "platform.fee")
public class PlatformFeeProperties {

    /** 매칭 1건당 이용료(원). 시범 운영 가정값 5,000원 */
    private int matching;

    /** 정식 가격 정책이 확정되기 전까지 값 조정이 가능한지 여부 */
    private boolean policyEditable;
}
