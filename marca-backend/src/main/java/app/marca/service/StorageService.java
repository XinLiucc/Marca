package app.marca.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象。本地磁盘是 MVP 实现；后续切到对象存储（OSS / S3）只换实现类。
 * 返回的 url 是可被前端直接 GET 的相对路径，业务表里直接持久化这个 url。
 */
public interface StorageService {

    /** 保存语音文件，返回访问 url（如 /uploads/voice/2026-06-25/123_1718000000.webm）。 */
    StoredFile storeVoice(long userId, MultipartFile file);

    record StoredFile(String url, long bytes) {}
}
