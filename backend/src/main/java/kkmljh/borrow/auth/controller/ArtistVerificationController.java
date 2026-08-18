package kkmljh.borrow.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.auth.dto.ArtistVerificationRequest;
import kkmljh.borrow.auth.dto.ArtistVerificationResponse;
import kkmljh.borrow.auth.service.ArtistVerificationService;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 예술가 인증 신청·상태 확인 (기능명세 1.2, 유저플로우 s2) */
@Tag(name = "예술가 인증", description = "예술가 인증 신청과 상태 확인 (기능명세 1.2)")
@RestController
@RequestMapping("/api/me/artist-verification")
@RequiredArgsConstructor
public class ArtistVerificationController {

    private final ArtistVerificationService artistVerificationService;

    @Operation(summary = "예술가 인증 신청",
            description = "ARTIST 계정만 신청할 수 있다(403). 거절된 신청은 다시 올릴 수 있고, "
                    + "심사 중이거나 이미 승인된 신청을 다시 올리면 409.")
    @PostMapping
    public ApiResponse<ArtistVerificationResponse> apply(@GuestId String guestId,
                                                         @Valid @RequestBody ArtistVerificationRequest request) {
        return ApiResponse.ok(artistVerificationService.apply(guestId, request));
    }

    @Operation(summary = "예술가 인증 상태 확인",
            description = "신청한 적이 없으면 404가 아니라 200 + status \"NONE\" 을 돌려준다.")
    @GetMapping
    public ApiResponse<ArtistVerificationResponse> status(@GuestId String guestId) {
        return ApiResponse.ok(artistVerificationService.status(guestId));
    }
}
