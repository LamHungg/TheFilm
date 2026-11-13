package vn.thefilm.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import vn.thefilm.backend.dto.MediaUploadResponse;
import vn.thefilm.backend.service.impl.MediaStorageServiceImpl;

class MediaStorageServiceTests {

    @TempDir
    Path mediaDir;

    @Test
    void storesImageUnderGeneratedNameAndReturnsPublicUrl() throws Exception {
        MediaStorageServiceImpl service = new MediaStorageServiceImpl(mediaDir);
        MockMultipartFile poster = new MockMultipartFile("file", "../../evil name.png", "image/png", new byte[] {1, 2, 3});

        MediaUploadResponse response = service.store(poster);

        assertThat(response.url()).startsWith("/media/").endsWith(".png").doesNotContain("evil");
        assertThat(response.contentType()).isEqualTo("image/png");
        Path stored = mediaDir.resolve(response.url().substring("/media/".length()));
        assertThat(Files.readAllBytes(stored)).containsExactly(1, 2, 3);
        try (var files = Files.list(mediaDir)) {
            assertThat(files).hasSize(1);
        }
    }

    @Test
    void rejectsUnsupportedType() {
        MediaStorageServiceImpl service = new MediaStorageServiceImpl(mediaDir);
        MockMultipartFile html = new MockMultipartFile("file", "x.html", "text/html", new byte[] {1});

        assertThatThrownBy(() -> service.store(html)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsImageOverFiveMegabytes() {
        MediaStorageServiceImpl service = new MediaStorageServiceImpl(mediaDir);
        MockMultipartFile big = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.store(big))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void rejectsEmptyFile() {
        MediaStorageServiceImpl service = new MediaStorageServiceImpl(mediaDir);
        MockMultipartFile empty = new MockMultipartFile("file", "a.mp4", "video/mp4", new byte[0]);

        assertThatThrownBy(() -> service.store(empty)).isInstanceOf(IllegalArgumentException.class);
    }
}
