package vn.thefilm.backend.service;

import org.springframework.web.multipart.MultipartFile;

import vn.thefilm.backend.dto.MediaUploadResponse;

public interface MediaStorageService {

    /** Luu anh poster/banner hoac video trailer, tra ve URL public duoi /media/. */
    MediaUploadResponse store(MultipartFile file);
}
