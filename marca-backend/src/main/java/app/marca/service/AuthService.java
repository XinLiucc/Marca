package app.marca.service;

import app.marca.config.ApiException;
import app.marca.dto.LoginRequest;
import app.marca.dto.LoginResponse;
import app.marca.dto.RegisterRequest;
import app.marca.entity.User;
import app.marca.repository.UserRepository;
import app.marca.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int ID_RETRY_MAX = 3;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserIdGenerator userIdGenerator;

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "该邮箱已被注册");
        }
        String nickname = (req.getNickname() == null || req.getNickname().isBlank())
                ? defaultNickname(req.getEmail())
                : req.getNickname();
        String hashed = passwordEncoder.encode(req.getPassword());

        DataIntegrityViolationException lastError = null;
        for (int attempt = 0; attempt < ID_RETRY_MAX; attempt++) {
            User user = User.builder()
                    .id(userIdGenerator.next())
                    .email(req.getEmail())
                    .password(hashed)
                    .nickname(nickname)
                    .build();
            try {
                return userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException e) {
                // 极小概率：id 撞库；email 唯一冲突在前面已挡，这里只可能是 id 冲突
                lastError = e;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ID_GENERATION_FAILED",
                "用户 ID 生成连续冲突，请稍后重试");
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "邮箱或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "邮箱或密码错误");
        }
        String token = jwtService.issue(user.getId(), user.getEmail());
        return new LoginResponse(token, user.getNickname());
    }

    private String defaultNickname(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
