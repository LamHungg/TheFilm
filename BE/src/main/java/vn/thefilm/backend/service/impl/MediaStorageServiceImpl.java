package vn.thefilm.backend.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import vn.thefilm.backend.dto.MediaUploadResponse;
import vn.thefilm.backend.service.MediaStorageService;

@Service
public class MediaStorageServiceImpl implements MediaStorageService {

    public static final String PUBLIC_PREFIX = "/media/";

    static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    static final long MAX_VIDEO_BYTES = 200L * 1024 * 1024;

    // Ten file luu tru lay duoi tu content type da duyet, khong dung ten file client gui len.
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "video/mp4", "mp4",
            "video/webm", "webm");

    private final Path mediaDir;

    public MediaStorageServiceImpl(@Value("${app.media.dir:uploads}") Path mediaDir) {
        this.mediaDir = mediaDir.toAbsolutePath().normalize();
    }

    @Override
    public MediaUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn tệp để tải lên.");
        }
        String contentType = file.getContentType();
        String extension = contentType == null ? null : EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG, PNG, WebP hoặc video MP4, WebM.");
        }
        boolean video = contentType.startsWith("video/");
        long limit = video ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES;
        if (file.getSize() > limit) {
            throw new IllegalArgumentException(video
                    ? "Video vượt quá dung lượng tối đa 200 MB."
                    : "Ảnh vượt quá dung lượng tối đa 5 MB.");
        }

        String fileName = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(mediaDir);
            Files.copy(in, mediaDir.resolve(fileName));
        } catch (IOException exception) {
            throw new UncheckedIOException("Không lưu được tệp tải lên.", exception);
        }
        return new MediaUploadResponse(PUBLIC_PREFIX + fileName, contentType, file.getSize());
    }

    public Path mediaDir() {
        return mediaDir;
    }
}
