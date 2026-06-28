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

    @Valid
    private List<ImageInput> images;

    /** 用户自由记录的文本（"我还想说"），可空 */
    private String freeText;

    /** 天气 key（如 "sunny"），可空 */
    private String weather;

    /** 心情 key 列表（如 ["happy","tired"]），可空 */
    private List<String> moods;
}
