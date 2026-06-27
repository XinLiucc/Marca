package app.marca.controller;

import app.marca.config.ApiException;
import app.marca.dto.DailyQuestionsResponse;
import app.marca.dto.QuestionDto;
import app.marca.security.UserPrincipal;
import app.marca.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final QuestionService questionService;

    @GetMapping("/today")
    public DailyQuestionsResponse today(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(value = "_at", required = false) String atOverride
    ) {
        if (count < 1 || count > 7) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COUNT", "count 需在 1~7 之间");
        }
        LocalDateTime now = parseAt(atOverride);
        LocalDate today = now.toLocalDate();
        var questions = questionService.pickDaily(user.id(), today, count, now)
                .stream().map(QuestionDto::from).toList();
        return new DailyQuestionsResponse(today, questions);
    }

    /**
     * 测试钩子：?_at=2026-06-28T02:30 即可模拟该时刻的出题。
     * 生产环境上线前考虑加权限或环境标志，目前仅 dev 期方便验证。
     */
    private LocalDateTime parseAt(String atOverride) {
        if (atOverride == null || atOverride.isBlank()) {
            return LocalDateTime.now(ZONE);
        }
        try {
            return LocalDateTime.parse(atOverride);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AT",
                    "_at 需为 ISO 格式如 2026-06-28T02:30");
        }
    }
}
