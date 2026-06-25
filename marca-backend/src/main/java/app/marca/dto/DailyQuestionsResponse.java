package app.marca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class DailyQuestionsResponse {
    private LocalDate date;
    private List<QuestionDto> questions;
}
