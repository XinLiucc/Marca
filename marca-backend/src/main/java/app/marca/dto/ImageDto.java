package app.marca.dto;

import app.marca.entity.RecordImage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageDto {
    private Long id;
    private String url;
    private Integer width;
    private Integer height;
    private Integer bytes;
    private Integer sortOrder;

    public static ImageDto from(RecordImage img) {
        return new ImageDto(img.getId(), img.getUrl(), img.getWidth(), img.getHeight(),
                img.getBytes(), img.getSortOrder());
    }
}
