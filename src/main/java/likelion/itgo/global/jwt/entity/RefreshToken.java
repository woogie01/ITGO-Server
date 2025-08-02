package likelion.itgo.global.jwt.entity;

import jakarta.persistence.*;
import likelion.itgo.global.support.BaseTimeEntity;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, length = 500)
    private String token;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public void updateToken(String token, LocalDateTime expiredAt) {
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

}