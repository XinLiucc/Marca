package app.marca.controller;

import app.marca.config.ApiException;
import app.marca.dto.DailyQuestionsResponse;
import app.marca.dto.QuestionDto;
import app.marca.security.UserPrincipal;
import app.marca.service.BackfillPolicy;
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
import java.time.LocalTime;
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
            @RequestParam(defaultValue = "5") int count
    ) {
        if (count < 1 || count > 7) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COUNT", "count 需在 1~7 之间");
        }
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDate today = now.toLocalDate();
        var questions = questionService.pickDaily(user.id(), today, count, now)
                .stream().map(QuestionDto::from).toList();
        return new DailyQuestionsResponse(today, questions);
    }

    /**
     * 补写过去忘记的日子专用出题：date 只能落在补写窗口内（不开放任意日期），
     * seed 按 date 稳定；场景匹配的时段用「目标日期的日历 + 真实当下的钟点」拼出来——
     * 周几 / 季节是那天的客观事实，但时段只能是此刻真正在写的时刻，没法伪造那天几点写的。
     */
    @GetMapping("/backfill")
    public DailyQuestionsResponse backfill(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam LocalDate date,
            @RequestParam(defaultValue = "5") int count
    ) {
        if (count < 1 || count > 7) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COUNT", "count 需在 1~7 之间");
        }
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (!BackfillPolicy.isWithinWindow(now.toLocalDate(), date)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OUT_OF_BACKFILL_WINDOW",
                    "太久之前的日子不能补写了");
        }
        LocalTime clockNow = now.toLocalTime();
        LocalDateTime ctxNow = LocalDateTime.of(date, clockNow);
        var questions = questionService.pickDaily(user.id(), date, count, ctxNow)
                .stream().map(QuestionDto::from).toList();
        return new DailyQuestionsResponse(date, questions);
    }
}
