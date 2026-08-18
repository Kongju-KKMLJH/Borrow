package kkmljh.borrow.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode · BusinessException")
class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("모든 에러코드는 상태값과 메시지를 갖는다 (필터에서 리터럴 없이 응답을 만들 수 있어야 한다)")
    void everyCodeHasStatusAndMessage(ErrorCode code) {
        assertThat(code.getStatus()).isNotNull();
        assertThat(code.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("인증/인가 코드의 상태값 — 401 UNAUTHORIZED, 403 FORBIDDEN")
    void authStatuses() {
        assertThat(ErrorCode.UNAUTHORIZED.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.FORBIDDEN.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("회원 관련 코드의 상태값 — 409 중복 아이디, 404 회원 없음")
    void userStatuses() {
        assertThat(ErrorCode.DUPLICATE_LOGIN_ID.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.USER_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("NOT_FOUND 계열은 모두 404")
    void notFoundStatuses() {
        assertThat(ErrorCode.ACTIVITY_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.SPACE_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.SLOT_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.REQUEST_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.PARTICIPATION_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("중복·상태 충돌 계열은 409")
    void conflictStatuses() {
        assertThat(ErrorCode.ALREADY_PARTICIPATED.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.ALREADY_REQUESTED.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.REQUEST_ALREADY_HANDLED.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.SPACE_HAS_REQUESTS.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("X-Guest-Id 시절의 GUEST_ID_REQUIRED 는 제거되어 401 UNAUTHORIZED 로 통일됐다")
    void guestIdRequiredIsGone() {
        assertThat(ErrorCode.values())
                .extracting(Enum::name)
                .doesNotContain("GUEST_ID_REQUIRED");
    }

    @Test
    @DisplayName("BusinessException 은 코드의 기본 메시지를 그대로 쓴다")
    void businessExceptionDefaultMessage() {
        BusinessException e = new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        assertThat(e.getMessage()).isEqualTo("활동을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("BusinessException 은 상황별 메시지로 덮어쓸 수 있다")
    void businessExceptionCustomMessage() {
        BusinessException e = new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(e.getMessage()).isEqualTo("종료 시각은 시작 시각보다 늦어야 합니다.");
    }
}
