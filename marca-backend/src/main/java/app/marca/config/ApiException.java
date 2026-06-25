package app.marca.config;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 业务异常基类。throw 即可，由 GlobalExceptionHandler 统一转成 HTTP 响应。 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
