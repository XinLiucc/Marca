package app.marca.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SaveRecordRequest {

    @NotNull(message = "recordDate 不能为空")
    private LocalDate recordDate;

    @Valid
    private List<AnswerInput> answers;

    private String voiceUrl;

    private Integer voiceDuration;
}
