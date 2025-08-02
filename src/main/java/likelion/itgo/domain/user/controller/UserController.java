package likelion.itgo.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import likelion.itgo.domain.user.dto.SignUpRequest;
import likelion.itgo.domain.user.dto.SignUpResponse;
import likelion.itgo.domain.user.service.UserService;
import likelion.itgo.global.error.GlobalErrorCode;
import likelion.itgo.global.error.exception.CustomException;
import likelion.itgo.global.jwt.dto.UserDetailsImpl;
import likelion.itgo.global.resolver.CurrentMemberId;
import likelion.itgo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Tag(name = "User API", description = "User 관련 API")
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "신규 사용자를 등록합니다.")
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("회원가입 요청 - loginId: {}", request.loginId());

        SignUpResponse response = userService.signUp(request);
        return ApiResponse.success(response, "회원가입 성공");
    }

}