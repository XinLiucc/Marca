package app.marca.dto;

import app.marca.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestionDto {
    private Long id;
    private Question.Category category;
    private String content;

    public static QuestionDto from(Question q) {
        return new QuestionDto(q.getId(), q.getCategory(), q.getContent());
    }
}
