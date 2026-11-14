package vn.thefilm.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.dto.MediaUploadResponse;
import vn.thefilm.backend.service.MediaStorageService;

@RestController
@RequestMapping("/api/v1/admin/media")
@PreAuthorize("hasRole('CHAIN_ADMIN')")
public class AdminMediaController {

    private final MediaStorageService mediaStorageService;

    public AdminMediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(mediaStorageService.store(file));
    }
}
