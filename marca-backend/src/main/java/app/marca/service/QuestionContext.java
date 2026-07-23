package app.marca.service;

import cn.hutool.core.date.ChineseDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

/**
 * 出题上下文：当前时段 / 周几 / 季节 / 节日。
 * 用来跟 question.tags 匹配，决定哪些题被前置作为「钩子」。
 */
public record QuestionContext(String timeOfDay, String dayOfWeek, String dayGroup, String season, String holiday) {

    public static QuestionContext from(LocalDateTime now) {
        return new QuestionContext(
                timeOfDayOf(now.getHour()),
                dayOfWeekOf(now.getDayOfWeek().getValue()),
                dayGroupOf(now.getDayOfWeek().getValue()),
                seasonOf(now.getMonth()),
                holidayOf(now.toLocalDate())
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

    /** 阳历节日优先判断，命中即返回；没有再查农历节日（春节等年年阳历日期不同）。 */
    private static String holidayOf(LocalDate date) {
        String solar = solarHolidayOf(date.getMonthValue(), date.getDayOfMonth());
        if (solar != null) return solar;
        return lunarHolidayOf(date);
    }

    private static String solarHolidayOf(int month, int day) {
        if (month == 1 && day == 1) return "new_year";
        if (month == 2 && day == 14) return "valentines_day";
        if (month == 3 && day == 8) return "womens_day";
        if (month == 5 && day == 1) return "labor_day";
        if (month == 6 && day == 1) return "childrens_day";
        if (month == 9 && day == 10) return "teachers_day";
        if (month == 10 && day == 1) return "national_day";
        if (month == 11 && day == 11) return "singles_day";
        if (month == 12 && day == 24) return "christmas_eve";
        if (month == 12 && day == 25) return "christmas";
        return null;
    }

    /** 清明是节气不是固定农历日期，算法更复杂，这一轮先不做，留到后面单独处理。 */
    private static String lunarHolidayOf(LocalDate date) {
        ChineseDate chineseDate = new ChineseDate(toDate(date));
        int lunarMonth = chineseDate.getMonth();
        int lunarDay = chineseDate.getDay();
        if (lunarMonth == 1 && lunarDay == 1) return "spring_festival";
        if (lunarMonth == 1 && lunarDay == 15) return "lantern_festival";
        if (lunarMonth == 5 && lunarDay == 5) return "dragon_boat_festival";
        if (lunarMonth == 8 && lunarDay == 15) return "mid_autumn_festival";
        // 除夕：农历十二月最后一天，天数是 29 还是 30 逐年不同，用"明天是不是正月初一"来判断更稳
        ChineseDate tomorrow = new ChineseDate(toDate(date.plusDays(1)));
        if (tomorrow.getMonth() == 1 && tomorrow.getDay() == 1) return "new_year_eve";
        return null;
    }

    /** java.sql.Date.valueOf() 转出来的 Date 没有真正的时刻，Hutool 内部 toInstant() 会直接抛异常。 */
    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
