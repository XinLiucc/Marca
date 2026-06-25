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
            @RequestParam(defaultValue = "3") int count
    ) {
        if (count < 1 || count > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COUNT", "count 需在 1~5 之间");
        }
        LocalDate today = LocalDate.now(ZONE);
        var questions = questionService.pickDaily(user.id(), today, count)
                .stream().map(QuestionDto::from).toList();
        return new DailyQuestionsResponse(today, questions);
    }
}
