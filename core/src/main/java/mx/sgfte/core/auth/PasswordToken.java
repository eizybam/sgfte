package mx.sgfte.core.auth;

import java.time.LocalDateTime;

public record PasswordToken(long id, long appUserId, String token, LocalDateTime expiresAt, LocalDateTime usedAt) {
    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
