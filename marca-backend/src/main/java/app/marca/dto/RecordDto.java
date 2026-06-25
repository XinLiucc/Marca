package app.marca.dto;

import app.marca.entity.Record;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class RecordDto {
    private Long id;
    private LocalDate recordDate;
    private List<AnswerDto> answers;
    private String voiceUrl;
    private Integer voiceDuration;
    private List<ImageDto> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RecordDto from(Record r) {
        List<AnswerDto> answers = r.getAnswers().stream().map(AnswerDto::from).toList();
        List<ImageDto> images = r.getImages().stream().map(ImageDto::from).toList();
        return new RecordDto(r.getId(), r.getRecordDate(), answers,
                r.getVoiceUrl(), r.getVoiceDuration(), images,
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
