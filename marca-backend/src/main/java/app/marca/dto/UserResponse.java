package app.marca.dto;

import app.marca.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt());
    }
}
