package kkmljh.borrow.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),

    // 인증 / 회원
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),

    // 활동 (C)
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "활동을 찾을 수 없습니다."),
    ACTIVITY_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "모집 중인 활동이 아닙니다."),
    CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "모집 인원이 가득 찼습니다."),
    ALREADY_PARTICIPATED(HttpStatus.CONFLICT, "이미 참여 신청한 활동입니다."),
    PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "참여 신청 내역이 없습니다."),

    // 공간 / 개최요청 (B)
    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "공간을 찾을 수 없습니다."),
    SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "유휴 시간대를 찾을 수 없습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "개최 요청을 찾을 수 없습니다."),
    SPACE_HAS_REQUESTS(HttpStatus.CONFLICT, "개최 요청이 있는 공간은 삭제할 수 없습니다."),
    DUPLICATE_SPACE(HttpStatus.CONFLICT, "이미 등록한 공간입니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "이미 개최 요청이 존재합니다."),
    REQUEST_ALREADY_HANDLED(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),

    // AI (A)
    AI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "활동 분석에 실패했습니다."),
    NO_MATCHING_SPACE(HttpStatus.NOT_FOUND, "조건에 맞는 공간이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
