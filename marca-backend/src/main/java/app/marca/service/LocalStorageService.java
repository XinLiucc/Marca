package app.marca.service;

import app.marca.config.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> ALLOWED_EXT = Set.of("webm", "m4a", "mp4", "mp3", "wav", "ogg");

    private final Path uploadDir;
    private final String urlPrefix;

    public LocalStorageService(
            @Value("${marca.upload.dir}") String uploadDirProp,
            @Value("${marca.upload.url-prefix}") String urlPrefix
    ) {
        this.uploadDir = Paths.get(uploadDirProp).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    @Override
    public StoredFile storeVoice(long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "上传文件为空");
        }
        String ext = extOf(file.getOriginalFilename(), file.getContentType());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "BAD_EXTENSION",
                    "仅支持 " + String.join(", ", ALLOWED_EXT));
        }

        LocalDate today = LocalDate.now(ZONE);
        String dayDir = today.format(DATE_FMT);
        String filename = userId + "_" + System.currentTimeMillis() + "." + ext;
        Path target = uploadDir.resolve("voice").resolve(dayDir).resolve(filename);

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("voice upload failed for user {}", userId, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_FAILED",
                    "文件保存失败");
        }

        String url = urlPrefix + "/voice/" + dayDir + "/" + filename;
        return new StoredFile(url, file.getSize());
    }

    private String extOf(String filename, String contentType) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                return filename.substring(dot + 1).toLowerCase();
            }
        }
        // 兜底：从 contentType 推断
        if (contentType == null) return "";
        return switch (contentType) {
            case "audio/webm" -> "webm";
            case "audio/mp4", "audio/x-m4a" -> "m4a";
            case "audio/mpeg" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/ogg" -> "ogg";
            default -> "";
        };
    }
}
