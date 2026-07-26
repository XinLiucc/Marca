package app.marca.service;

import app.marca.config.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService newService() {
        return new LocalStorageService(tempDir.toString(), "/uploads");
    }

    @Test
    void storeVoice_emptyFile_throwsEmptyFile() {
        LocalStorageService service = newService();
        MockMultipartFile file = new MockMultipartFile("file", "a.webm", "audio/webm", new byte[0]);

        ApiException ex = assertThrows(ApiException.class, () -> service.storeVoice(1L, file));
        assertEquals("EMPTY_FILE", ex.getCode());
    }

    @Test
    void storeVoice_disallowedExtension_throwsBadExtension() {
        LocalStorageService service = newService();
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", "x".getBytes());

        ApiException ex = assertThrows(ApiException.class, () -> service.storeVoice(1L, file));
        assertEquals("BAD_EXTENSION", ex.getCode());
    }

    @Test
    void storeVoice_validExtension_savesFileToDiskAndReturnsUrl() throws IOException {
        LocalStorageService service = newService();
        byte[] content = "fake-audio-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "a.webm", "audio/webm", content);

        StorageService.StoredFile stored = service.storeVoice(42L, file);

        assertTrue(stored.url().matches("^/uploads/voice/\\d{4}-\\d{2}-\\d{2}/42_\\d+\\.webm$"));
        assertEquals(content.length, stored.bytes());
        // url 是相对路径，去掉前缀 "/uploads/" 就是相对 tempDir 的真实落盘路径
        Path saved = tempDir.resolve(stored.url().substring("/uploads/".length()));
        assertTrue(Files.exists(saved));
        assertEquals(content.length, Files.size(saved));
    }

    @Test
    void storeVoice_noFilenameExtension_fallsBackToContentType() throws IOException {
        LocalStorageService service = newService();
        // 文件名没有后缀，只能靠 contentType 推断扩展名
        MockMultipartFile file = new MockMultipartFile("file", "recording", "audio/mp4", "x".getBytes());

        StorageService.StoredFile stored = service.storeVoice(1L, file);

        assertTrue(stored.url().endsWith(".m4a"));
    }

    @Test
    void storeImage_validPng_parsesWidthAndHeight() throws IOException {
        LocalStorageService service = newService();
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", pngBytes(30, 20));

        StorageService.StoredImage stored = service.storeImage(7L, file);

        assertEquals(30, stored.width());
        assertEquals(20, stored.height());
    }

    @Test
    void storeImage_corruptImageBytes_widthAndHeightNullButNoException() {
        LocalStorageService service = newService();
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "not-really-a-png".getBytes());

        StorageService.StoredImage stored = service.storeImage(1L, file);

        assertNull(stored.width());
        assertNull(stored.height());
    }

    private static byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
