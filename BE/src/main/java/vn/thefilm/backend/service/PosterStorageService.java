package vn.thefilm.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PosterStorageService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png");

    private final Path posterDirectory;

    public PosterStorageService(
            @Value("${thefilm.media.poster-directory:${user.home}/.thefilm/media/posters}") String posterDirectory) {
        this.posterDirectory = Path.of(posterDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh poster.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh poster không được vượt quá 10 MB.");
        }

        try (ImageInputStream imageInput = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp tải lên không phải ảnh PNG hoặc JPEG hợp lệ.");
            }
            ImageReader reader = readers.next();
            String format;
            try {
                reader.setInput(imageInput, true, true);
                format = reader.getFormatName().toLowerCase(Locale.ROOT);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > 10000 || height > 10000 || (long) width * height > 40_000_000L) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước ảnh poster vượt giới hạn cho phép.");
                }
            } finally {
                reader.dispose();
            }
            if (!ALLOWED_FORMATS.contains(format)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ ảnh PNG và JPEG.");
            }

            Files.createDirectories(posterDirectory);
            String extension = format.equals("jpeg") ? ".jpg" : ".png";
            String fileName = UUID.randomUUID() + extension;
            Path destination = posterDirectory.resolve(fileName).normalize();
            if (!destination.startsWith(posterDirectory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tệp ảnh không hợp lệ.");
            }
            try (var input = file.getInputStream()) {
                Files.copy(input, destination);
            }
            return "/api/v1/public/media/posters/" + fileName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể lưu ảnh poster vào kho lưu trữ.", exception);
        }
    }

    public Path resolve(String fileName) {
        if (!StringUtils.hasText(fileName) || fileName.contains("/") || fileName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path path = posterDirectory.resolve(fileName).normalize();
        if (!path.startsWith(posterDirectory) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return path;
    }

}
