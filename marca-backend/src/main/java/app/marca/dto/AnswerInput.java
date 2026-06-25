package app.marca.dto;

import app.marca.entity.Question;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnswerInput {

    private Long questionId;

    @NotBlank(message = "问题文本不能为空")
    @Size(max = 255)
    private String question;

    private Question.Category category;

    @NotBlank(message = "回答不能为空")
    private String answer;
}
