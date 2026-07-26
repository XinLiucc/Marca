package app.marca.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackfillPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 26);

    @Test
    void today_isWithinWindow() {
        assertTrue(BackfillPolicy.isWithinWindow(TODAY, TODAY));
    }

    @Test
    void exactlyWindowDaysAgo_isWithinWindow() {
        // WINDOW_DAYS = 3，today - 3 是窗口内的边界，应该允许
        assertTrue(BackfillPolicy.isWithinWindow(TODAY, TODAY.minusDays(3)));
    }

    @Test
    void oneDayBeyondWindow_isOutOfWindow() {
        // today - 4 超出窗口，应该拒绝
        assertFalse(BackfillPolicy.isWithinWindow(TODAY, TODAY.minusDays(4)));
    }

    @Test
    void futureDate_isOutOfWindow() {
        // target 在 today 之后，即使只差1天，也不算窗口内
        assertFalse(BackfillPolicy.isWithinWindow(TODAY, TODAY.plusDays(1)));
    }
}
