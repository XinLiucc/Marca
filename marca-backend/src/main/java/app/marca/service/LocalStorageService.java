package app.marca.service;

import app.marca.config.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    private static final Set<String> ALLOWED_VOICE_EXT = Set.of("webm", "m4a", "mp4", "mp3", "wav", "ogg");
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

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
        Path target = saveToCategory(userId, file, "voice", ALLOWED_VOICE_EXT);
        String url = relativeUrl(target);
        return new StoredFile(url, file.getSize());
    }

    @Override
    public StoredImage storeImage(long userId, MultipartFile file) {
        Path target = saveToCategory(userId, file, "image", ALLOWED_IMAGE_EXT);
        Integer w = null;
        Integer h = null;
        try {
            BufferedImage img = ImageIO.read(target.toFile());
            if (img != null) {
                w = img.getWidth();
                h = img.getHeight();
            }
        } catch (IOException e) {
            log.warn("image dimension parse failed for {}: {}", target, e.getMessage());
        }
        return new StoredImage(relativeUrl(target), file.getSize(), w, h);
    }

    private Path saveToCategory(long userId, MultipartFile file, String category, Set<String> allowed) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "上传文件为空");
        }
        String ext = extOf(file.getOriginalFilename(), file.getContentType());
        if (!allowed.contains(ext)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "BAD_EXTENSION",
                    "仅支持 " + String.join(", ", allowed));
        }
        LocalDate today = LocalDate.now(ZONE);
        String dayDir = today.format(DATE_FMT);
        String filename = userId + "_" + System.currentTimeMillis() + "." + ext;
        Path target = uploadDir.resolve(category).resolve(dayDir).resolve(filename);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("{} upload failed for user {}", category, userId, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_FAILED",
                    "文件保存失败");
        }
        return target;
    }

    private String relativeUrl(Path target) {
        // /uploads/voice/2026-06-25/xxx.webm
        Path rel = uploadDir.relativize(target);
        return urlPrefix + "/" + rel.toString().replace('\\', '/');
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
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "";
        };
    }
}
