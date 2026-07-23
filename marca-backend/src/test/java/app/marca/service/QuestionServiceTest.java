package app.marca.service;

import app.marca.entity.Question;
import app.marca.repository.QuestionRepository;
import app.marca.repository.RecordAnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private RecordAnswerRepository recordAnswerRepository;

    @InjectMocks
    private QuestionService questionService;

    private static Question tagged(long id, String key, String value) {
        return Question.builder()
                .id(id)
                .category(Question.Category.event)
                .content("q" + id)
                .tags(Map.of(key, List.of(value)))
                .build();
    }

    // ---------- specialPoolOf 优先级：节日 > 周几 > 季节 ----------

    @Test
    void specialPoolOf_holidayPresent_ignoresDayAndSeasonMatches() {
        Question holidayQ = tagged(1, "holiday", "national_day");
        Question dayQ = tagged(2, "day", "weekday");
        Question seasonQ = tagged(3, "season", "autumn");
        List<Question> all = List.of(holidayQ, dayQ, seasonQ);

        // 2026-10-01 国庆节，同时是当年真实的某个周几（day 标签用 weekday/weekend 兜底保证一定命中）
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 10, 1, 10, 0));

        List<Question> special = questionService.specialPoolOf(all, ctx);

        assertEquals(List.of(holidayQ), special);
    }

    @Test
    void specialPoolOf_noHoliday_dayTakesPriorityOverSeason() {
        Question dayQ = tagged(2, "day", "weekend");
        Question seasonQ = tagged(3, "season", "summer");
        List<Question> all = List.of(dayQ, seasonQ);

        // 2026-07-25 是周六（非节日），summer 季节
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 7, 25, 10, 0));

        List<Question> special = questionService.specialPoolOf(all, ctx);

        assertEquals(List.of(dayQ), special);
    }

    @Test
    void specialPoolOf_noHolidayOrDayMatch_fallsBackToSeason() {
        Question seasonQ = tagged(3, "season", "summer");
        List<Question> all = List.of(seasonQ);

        // 2026-07-21 是周二，非节日、非 monday/friday/weekend
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 7, 21, 10, 0));

        List<Question> special = questionService.specialPoolOf(all, ctx);

        assertEquals(List.of(seasonQ), special);
    }

    // ---------- pickDaily：时段钩子固定占位 + 去重 ----------

    @Test
    void pickDaily_timeMatchedQuestion_alwaysIncludedAsHook() {
        Question morningQ = tagged(1, "time", "morning");
        List<Question> general = List.of(
                Question.builder().id(2L).category(Question.Category.emotion).content("g2").build(),
                Question.builder().id(3L).category(Question.Category.future).content("g3").build(),
                Question.builder().id(4L).category(Question.Category.event).content("g4").build());
        List<Question> all = new java.util.ArrayList<>();
        all.add(morningQ);
        all.addAll(general);

        when(questionRepository.findAll()).thenReturn(all);
        when(recordAnswerRepository.findRecentQuestionIds(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of());

        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 8, 0); // 周二上午，非节日
        List<Question> result = questionService.pickDaily(1L, LocalDate.of(2026, 7, 21), 4, now);

        List<Long> ids = result.stream().map(Question::getId).collect(Collectors.toList());
        assertTrue(ids.contains(1L), "时段命中的题应该总是被选中");
        assertEquals(4, result.size());
    }

    @Test
    void pickDaily_recentlyAnsweredQuestion_excludedUnlessPoolWouldBeEmpty() {
        Question morningA = tagged(1, "time", "morning");
        Question morningB = tagged(2, "time", "morning");
        List<Question> all = List.of(morningA, morningB);

        when(questionRepository.findAll()).thenReturn(all);
        // 用户最近答过 id=1，去重后时段池还剩 id=2 可用，应该优先选 2 而不是 1
        when(recordAnswerRepository.findRecentQuestionIds(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(1L));

        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 8, 0);
        List<Question> result = questionService.pickDaily(1L, LocalDate.of(2026, 7, 21), 1, now);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void pickDaily_recentlyAnsweredButPoolWouldBeEmpty_fallsBackToRepeat() {
        Question onlyQuestion = tagged(1, "time", "morning");
        List<Question> all = List.of(onlyQuestion);

        when(questionRepository.findAll()).thenReturn(all);
        // 唯一一道题最近答过；去重会导致无题可出，算法应该放弃去重而不是返回空
        when(recordAnswerRepository.findRecentQuestionIds(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(1L));

        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 8, 0);
        List<Question> result = questionService.pickDaily(1L, LocalDate.of(2026, 7, 21), 1, now);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }
}
