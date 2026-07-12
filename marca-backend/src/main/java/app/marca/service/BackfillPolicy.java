package app.marca.service;

import java.time.LocalDate;

/** 补写窗口：只能补最近 WINDOW_DAYS 天忘记写的日子，不能任意改写更早的历史。 */
public final class BackfillPolicy {

    public static final int WINDOW_DAYS = 3;

    private BackfillPolicy() {
    }

    public static boolean isWithinWindow(LocalDate today, LocalDate target) {
        return !target.isAfter(today) && !target.isBefore(today.minusDays(WINDOW_DAYS));
    }
}
