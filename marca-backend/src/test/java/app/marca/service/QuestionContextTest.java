package app.marca.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestionContextTest {

    @Test
    void from_lunarNewYear2026_resolvesSpringFestival() {
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 2, 17, 10, 0));
        assertEquals("spring_festival", ctx.holiday());
    }

    @Test
    void from_dayBeforeLunarNewYear2026_resolvesNewYearEve() {
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 2, 16, 10, 0));
        assertEquals("new_year_eve", ctx.holiday());
    }

    @Test
    void from_lanternFestival2026_resolvesLanternFestival() {
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 3, 3, 10, 0));
        assertEquals("lantern_festival", ctx.holiday());
    }

    @Test
    void from_dragonBoatFestival2026_resolvesDragonBoatFestival() {
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 6, 19, 10, 0));
        assertEquals("dragon_boat_festival", ctx.holiday());
    }

    @Test
    void from_midAutumnFestival2026_resolvesMidAutumnFestival() {
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 9, 25, 10, 0));
        assertEquals("mid_autumn_festival", ctx.holiday());
    }

    @Test
    void from_fixedSolarHoliday_resolvesNationalDay() {
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 10, 1, 10, 0));
        assertEquals("national_day", ctx.holiday());
    }

    @Test
    void from_ordinaryDay_holidayIsNull() {
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 7, 23, 10, 0));
        assertNull(ctx.holiday());
    }

    @Test
    void from_hourAndWeekday_resolvesTimeOfDayAndDayOfWeek() {
        // 2026-07-23 是周四
        QuestionContext ctx = QuestionContext.from(LocalDateTime.of(2026, 7, 23, 20, 0));
        assertEquals("evening", ctx.timeOfDay());
        assertEquals("thursday", ctx.dayOfWeek());
        assertEquals("weekday", ctx.dayGroup());
        assertEquals("summer", ctx.season());
    }
}
