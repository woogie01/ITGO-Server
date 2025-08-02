package likelion.itgo.domain.user.service;

import likelion.itgo.domain.member.entity.Member;
import likelion.itgo.domain.member.entity.Role;
import likelion.itgo.domain.member.repository.MemberRepository;
import likelion.itgo.domain.user.dto.*;
import likelion.itgo.domain.user.entity.User;
import likelion.itgo.domain.user.repository.UserRepository;
import likelion.itgo.global.error.GlobalErrorCode;
import likelion.itgo.global.error.exception.CustomException;
import likelion.itgo.global.jwt.JwtTokenProvider;
import likelion.itgo.global.jwt.entity.RefreshToken;
import likelion.itgo.global.jwt.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInSeconds;

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        // 1. 비밀번호 확인
        validatePasswordMatch(request);

        // 2. 중복 검증
        validateDuplicateLoginId(request.loginId());
        validateDuplicateUsername(request.username());

        // 3. Member 생성
        Member member = createMember(request);

        // 4. User 생성 + Member 관계 매핑
        User user = createUser(request, member);

        // 5. 저장
        Member savedMember = memberRepository.save(member);
        User savedUser = userRepository.save(user);

        // 6. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getId(), savedUser.getMember().getId(), savedMember.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId());

        // 7. 리프레시 토큰 저장
        saveRefreshToken(savedUser.getId(), refreshToken);
        log.info("회원가입 완료 - userId: {}, loginId: {}", savedUser.getId(), savedUser.getLoginId());

        return SignUpResponse.of(
                savedUser.getId(),
                savedMember.getId(),
                savedUser.getLoginId(),
                savedMember.getUsername(),
                savedMember.getRole(),
                accessToken,
                refreshToken
        );
    }

    /**
     * 로그인
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "존재하지 않는 로그인 아이디입니다."));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }

        // 3. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getMember().getId(), user.getMember().getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 4. 리프레시 토큰 저장/업데이트
        saveOrUpdateRefreshToken(user.getId(), refreshToken);
        log.info("로그인 완료 - userId: {}, loginId: {}", user.getId(), user.getLoginId());

        return LoginResponse.of(
                user.getId(),
                user.getMember().getId(),
                user.getMember().getUsername(),
                user.getMember().getRole(),
                accessToken,
                refreshToken
        );
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * 토큰 재발급
     */
    @Transactional
    public TokenRefreshResponse reissueToken(TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();

        // 1. 리프레시 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN, "유효하지 않은 Refresh 토큰입니다.");
        }

        // 2. DB에서 리프레시 토큰 조회
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        RefreshToken storedRefreshToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "Refresh 토큰을 찾을 수 없습니다."));

        // 3. 토큰 일치 및 만료 여부 확인
        if (!storedRefreshToken.getToken().equals(refreshToken) || storedRefreshToken.isExpired()) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN, "유효하지 않은 Refresh 토큰입니다.");
        }

        // 4. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "존재하지 않는 유저입니다."));

        // 5. 새로운 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getMember().getId(), user.getMember().getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 6. 리프레시 토큰 업데이트
        saveOrUpdateRefreshToken(user.getId(), newRefreshToken);
        log.info("토큰 재발급 완료 - userId: {}", userId);

        return TokenRefreshResponse.of(newAccessToken, newRefreshToken);
    }

    /**
     * 로그인 ID 중복 체크
     */
    public boolean isLoginIdDuplicated(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    /**
     * 사용자명 중복 체크
     */
    public boolean isUsernameDuplicated(String username) {
        return memberRepository.existsByUsername(username);
    }

    private void validatePasswordMatch(SignUpRequest request) {
        if (!request.passwordMatches()) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }
    }

    private void validateDuplicateLoginId(String loginId) {
        if (isLoginIdDuplicated(loginId)) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST, "이미 존재하는 로그인 아이디입니다.");
        }
    }

    private void validateDuplicateUsername(String username) {
        if (isUsernameDuplicated(username)) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST, "이미 존재하는 회원명입니다.");
        }
    }

    private Member createMember(SignUpRequest request) {
        return Member.builder()
                .username(request.username())
                .role(Role.ROLE_MEMBER) // 기본 권한은 일반화원으로 설정
                .build();
    }

    private User createUser(SignUpRequest request, Member member) {
        String encodedPassword = passwordEncoder.encode(request.password());

        return User.builder()
                .loginId(request.loginId())
                .password(encodedPassword)
                .member(member)
                .build();
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        LocalDateTime expiredAt = extractExpirationFromToken(refreshToken);
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(userId)
                .token(refreshToken)
                .expiredAt(expiredAt)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
    }

    private void saveOrUpdateRefreshToken(Long userId, String refreshToken) {
        LocalDateTime expiredAt = extractExpirationFromToken(refreshToken);

        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.updateToken(refreshToken, expiredAt),
                        () -> saveRefreshToken(userId, refreshToken)
                );
    }

    private LocalDateTime extractExpirationFromToken(String token) {
        try {
            Date expiration = jwtTokenProvider.getExpiration(token);
            return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            // 기본 만료 시간 : 디폴트 값으로 연장
            return LocalDateTime.now().plusSeconds(refreshTokenValidityInSeconds);
        }
    }
}