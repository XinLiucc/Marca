package app.marca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VoiceUploadResponse {
    private String voiceUrl;
    private Integer duration;  // 秒，由前端传入透传
    private long bytes;
}
