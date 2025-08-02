package likelion.itgo.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import likelion.itgo.domain.user.dto.*;
import likelion.itgo.domain.user.service.UserService;
import likelion.itgo.global.error.GlobalErrorCode;
import likelion.itgo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth API", description = "인증 관련 API")
public class AuthController {

    private final UserService userService;

    /**
     * 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인 후 Token 발급")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청 - loginId: {}", request.loginId());

        LoginResponse response = userService.login(request);
        return ApiResponse.success(response, "로그인 성공");
    }

    /**
     * 토큰 재발급
     */
    @PostMapping("/reissue")
    @Operation(
            summary = "Access/Refresh 토큰 재발급",
            description = "RefreshToken 기반으로 AccessToken 및 RefreshToken 재발급"
    )
    public ApiResponse<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("토큰 재발급 요청");

        TokenRefreshResponse response = userService.reissueToken(request);
        return ApiResponse.success(response, "토큰 재발급 성공");
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "RefreshToken 제거")
    public Object logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            log.info("로그아웃 요청 - userId: {}", userId);
            userService.logout(userId);
            return ApiResponse.success("로그아웃이 완료되었습니다.");
        }

        return ApiResponse.fail(GlobalErrorCode.INVALID_PERMISSION, "인증되지 않은 사용자입니다.");
    }

}