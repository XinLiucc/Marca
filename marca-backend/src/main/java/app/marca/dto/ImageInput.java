package app.marca.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageInput {

    @NotBlank(message = "url 不能为空")
    private String url;

    private Integer width;
    private Integer height;
    private Integer bytes;
}
