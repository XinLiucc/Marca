package app.marca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称不能超过 50 字")
    private String nickname;
}
