package app.marca.service;

import java.time.LocalDateTime;
import java.time.Month;

/**
 * 出题上下文：当前时段 / 周几 / 季节。
 * 用来跟 question.tags 匹配，决定哪些题被前置作为「钩子」。
 */
public record QuestionContext(String timeOfDay, String dayOfWeek, String dayGroup, String season) {

    public static QuestionContext from(LocalDateTime now) {
        return new QuestionContext(
                timeOfDayOf(now.getHour()),
                dayOfWeekOf(now.getDayOfWeek().getValue()),
                dayGroupOf(now.getDayOfWeek().getValue()),
                seasonOf(now.getMonth())
        );
    }

    private static String timeOfDayOf(int hour) {
        if (hour >= 6 && hour <= 11) return "morning";
        if (hour >= 12 && hour <= 17) return "afternoon";
        if (hour >= 18 && hour <= 22) return "evening";
        return "late_night"; // 23, 0-5
    }

    private static String dayOfWeekOf(int isoDow) {
        return switch (isoDow) {
            case 1 -> "monday";
            case 2 -> "tuesday";
            case 3 -> "wednesday";
            case 4 -> "thursday";
            case 5 -> "friday";
            case 6 -> "saturday";
            default -> "sunday";
        };
    }

    private static String dayGroupOf(int isoDow) {
        return (isoDow == 6 || isoDow == 7) ? "weekend" : "weekday";
    }

    private static String seasonOf(Month month) {
        return switch (month) {
            case MARCH, APRIL, MAY -> "spring";
            case JUNE, JULY, AUGUST -> "summer";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "autumn";
            default -> "winter"; // 12, 1, 2
        };
    }
}
