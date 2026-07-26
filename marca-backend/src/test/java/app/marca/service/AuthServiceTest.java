package app.marca.service;

import app.marca.config.ApiException;
import app.marca.dto.LoginRequest;
import app.marca.dto.LoginResponse;
import app.marca.dto.RegisterRequest;
import app.marca.entity.User;
import app.marca.repository.UserRepository;
import app.marca.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserIdGenerator userIdGenerator;

    @InjectMocks
    private AuthService authService;

    // ---------- register ----------

    @Test
    void register_emailAlreadyExists_throwsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("a@b.com");
        req.setPassword("123456");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(req));
        assertEquals("EMAIL_EXISTS", ex.getCode());
    }

    @Test
    void register_blankNickname_defaultsToEmailPrefix() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("someone@example.com");
        req.setPassword("123456");
        req.setNickname("  ");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userIdGenerator.next()).thenReturn(1L);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(req);

        assertEquals("someone", saved.getNickname());
    }

    @Test
    void register_withNickname_usesGivenNickname() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("someone@example.com");
        req.setPassword("123456");
        req.setNickname("小明");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userIdGenerator.next()).thenReturn(1L);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(req);

        assertEquals("小明", saved.getNickname());
    }

    @Test
    void register_idCollisionOnce_retriesAndSucceeds() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("someone@example.com");
        req.setPassword("123456");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userIdGenerator.next()).thenReturn(1L, 2L);
        // 第一次 id 撞库，第二次成功
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("dup id"))
                .thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(req);

        assertEquals(2L, saved.getId());
        verify(userRepository, times(2)).saveAndFlush(any(User.class));
    }

    @Test
    void register_idCollisionExceedsRetries_throwsIdGenerationFailed() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("someone@example.com");
        req.setPassword("123456");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userIdGenerator.next()).thenReturn(1L);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("dup id"));

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(req));

        assertEquals("ID_GENERATION_FAILED", ex.getCode());
        verify(userRepository, times(3)).saveAndFlush(any(User.class));
    }

    // ---------- login ----------

    @Test
    void login_emailNotFound_throwsUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nope@example.com");
        req.setPassword("123456");
        when(userRepository.findByEmail("nope@example.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(req));
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("wrong");
        User user = User.builder().id(1L).email("a@b.com").password("hashed").nickname("A").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(req));
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }

    @Test
    void login_success_returnsTokenAndNickname() {
        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("right");
        User user = User.builder().id(1L).email("a@b.com").password("hashed").nickname("A").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("right", "hashed")).thenReturn(true);
        when(jwtService.issue(1L, "a@b.com")).thenReturn("token-123");

        LoginResponse resp = authService.login(req);

        assertEquals("token-123", resp.getToken());
        assertEquals("A", resp.getNickname());
    }
}
