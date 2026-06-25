package app.marca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageUploadResponse {
    private String imageUrl;
    private Integer width;
    private Integer height;
    private long bytes;
}
