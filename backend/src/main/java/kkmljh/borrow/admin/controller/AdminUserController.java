package kkmljh.borrow.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.admin.dto.AdminUserRequest;
import kkmljh.borrow.admin.dto.AdminUserResponse;
import kkmljh.borrow.admin.dto.AdminVerificationResponse;
import kkmljh.borrow.admin.service.AdminUserService;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 콘솔 — 회원 관리 (기능명세 7.1).
 * 역할 검사는 {@code SecurityConfig} 의 {@code /api/admin/**} 한 줄이 담당하므로
 * 컨트롤러·서비스에서 다시 확인하지 않는다.
 */
@Tag(name = "관리자 콘솔 — 회원", description = "전체 회원 목록, 회원 생성·수정·삭제, 예술가 인증 승인 (기능명세 7.1)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "7.1.1 전체 회원 목록 조회",
            description = "관리자 계정까지 포함한 전체 목록. 역할·가입일·인증 상태를 함께 내려준다.")
    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> users() {
        return ApiResponse.ok(adminUserService.findAll());
    }

    @Operation(summary = "7.1.2 회원 생성",
            description = "실제로 로그인하고 서비스를 이용하는 회원을 만든다. 관리자(ADMIN) 역할은 만들 수 없다.")
    @PostMapping("/users")
    public ApiResponse<AdminUserResponse> createUser(@Valid @RequestBody AdminUserRequest req) {
        return ApiResponse.ok(adminUserService.create(req));
    }

    @Operation(summary = "7.1.2 회원 수정",
            description = "모든 실제 회원을 수정할 수 있다. 비밀번호를 비우면 기존 값을 유지하고 아이디는 바꾸지 않는다. "
                    + "관리자 계정은 대상이 아니다(403).")
    @PutMapping("/users/{userId}")
    public ApiResponse<AdminUserResponse> updateUser(@PathVariable Long userId,
                                                     @Valid @RequestBody AdminUserRequest req) {
        return ApiResponse.ok(adminUserService.update(userId, req));
    }

    @Operation(summary = "7.1.2 회원 삭제",
            description = "회원 행을 실제로 지운다. 이 회원의 참여 신청·개설 프로그램·등록 공간·인증 신청이 함께 삭제된다. "
                    + "삭제된 공간에서 개최하기로 했던 남의 프로그램은 거절 상태로 되돌아간다. "
                    + "관리자 계정은 대상이 아니다(403).")
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminUserService.delete(userId);
        return ApiResponse.ok();
    }

    @Operation(summary = "7.1.4 예술가 인증 신청 목록",
            description = "status 를 주지 않으면 심사 대기(PENDING) 신청만 조회한다.")
    @GetMapping("/artist-verifications")
    public ApiResponse<List<AdminVerificationResponse>> verifications(
            @RequestParam(required = false) ArtistVerificationStatus status) {
        return ApiResponse.ok(adminUserService.findVerifications(status));
    }

    @Operation(summary = "7.1.4 예술가 인증 승인",
            description = "심사 대기 신청만 승인할 수 있다. 이미 처리된 신청은 중복 승인하지 않는다.")
    @PostMapping("/artist-verifications/{verificationId}/approve")
    public ApiResponse<AdminVerificationResponse> approve(@PathVariable Long verificationId) {
        return ApiResponse.ok(adminUserService.approve(verificationId));
    }
}
