package app.marca.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一响应体：{ "error": <code>, "message": <text> }
 * 继承 ResponseEntityExceptionHandler 以复用 Spring 对 404 / 405 / 415 等框架异常的状态码识别，
 * 仅覆写返回体形态。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(body(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAny(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(body("INTERNAL_ERROR", "服务器内部错误"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(body("VALIDATION_FAILED", message));
    }

    /** 父类对 404/405/415/媒体类型/参数缺失等框架异常都会走到这里，统一替换为我们的 body 形态。 */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String code = status.name();  // e.g. NOT_FOUND, METHOD_NOT_ALLOWED
        String message = ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase();
        return ResponseEntity.status(status).headers(headers).body(body(code, message));
    }

    private Map<String, Object> body(String code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", code);
        map.put("message", message);
        return map;
    }
}
