package kkmljh.borrow.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.auth.dto.MeResponse;
import kkmljh.borrow.auth.dto.SignupRequest;
import kkmljh.borrow.auth.service.AuthService;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 회원가입 · 인증 확인 */
@Tag(name = "인증", description = "회원가입 및 로그인 확인 (HTTP Basic)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입",
            description = "로그인아이디·비밀번호·닉네임·역할(MEMBER/HOST/ARTIST)로 가입한다. 비로그인 호출 가능.")
    @PostMapping("/signup")
    public ApiResponse<MeResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @Operation(summary = "내 정보 조회 (로그인 확인)",
            description = "Authorization: Basic 헤더로 인증한다. 200이면 로그인 성공, 401이면 실패.")
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@GuestId String guestId) {
        return ApiResponse.ok(authService.me(guestId));
    }
}
