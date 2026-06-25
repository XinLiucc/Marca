package app.marca.security;

/** 注入到 SecurityContext 的当前用户身份。Controller 用 @AuthenticationPrincipal UserPrincipal 接收。 */
public record UserPrincipal(Long id, String email) {
}
