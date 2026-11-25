package vn.thefilm.backend.controller;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.thefilm.backend.service.PosterStorageService;

@RestController
@RequestMapping("/api/v1/public/media/posters")
public class PosterMediaController {
    private final PosterStorageService posterStorageService;

    public PosterMediaController(PosterStorageService posterStorageService) {
        this.posterStorageService = posterStorageService;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getPoster(@PathVariable String fileName) throws IOException {
        Path path = posterStorageService.resolve(fileName);
        MediaType mediaType = fileName.toLowerCase().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}
