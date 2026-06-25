package app.marca.dto;

import app.marca.entity.Question;
import app.marca.entity.RecordAnswer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerDto {
    private Long id;
    private Long questionId;
    private String question;
    private Question.Category category;
    private String answer;
    private Integer sortOrder;

    public static AnswerDto from(RecordAnswer a) {
        return new AnswerDto(a.getId(), a.getQuestionId(), a.getQuestion(),
                a.getCategory(), a.getAnswer(), a.getSortOrder());
    }
}
