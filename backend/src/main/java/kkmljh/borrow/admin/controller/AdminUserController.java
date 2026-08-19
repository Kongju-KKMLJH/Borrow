package kkmljh.borrow.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kkmljh.borrow.admin.dto.AdminUserResponse;
import kkmljh.borrow.admin.dto.AdminVerificationResponse;
import kkmljh.borrow.admin.service.AdminUserService;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 콘솔 — 회원 관리 (기능명세 7.1).
 * 역할 검사는 {@code SecurityConfig} 의 {@code /api/admin/**} 한 줄이 담당하므로
 * 컨트롤러·서비스에서 다시 확인하지 않는다.
 */
@Tag(name = "관리자 콘솔 — 회원", description = "전체 회원 목록, 강제 탈퇴, 예술가 인증 승인 (기능명세 7.1)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "7.1.1 전체 회원 목록 조회",
            description = "탈퇴 회원까지 포함한 관리용 목록. 역할·가입일·인증 상태·임시 회원 여부·탈퇴 상태를 함께 내려준다.")
    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> users() {
        return ApiResponse.ok(adminUserService.findAll());
    }

    @Operation(summary = "7.1.3 회원 강제 탈퇴",
            description = "대상 회원의 로그인과 서비스 이용을 차단한다. 데이터는 지우지 않고 비활성 상태로 남긴다.")
    @PostMapping("/users/{userId}/withdraw")
    public ApiResponse<AdminUserResponse> withdraw(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.withdraw(userId));
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
