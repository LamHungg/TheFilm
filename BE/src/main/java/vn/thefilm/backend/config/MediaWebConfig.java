package vn.thefilm.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import vn.thefilm.backend.service.impl.MediaStorageServiceImpl;

/** Phuc vu tep da tai len (poster, trailer) tai /media/** de FE hien thi truc tiep. */
@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    private final MediaStorageServiceImpl mediaStorage;

    public MediaWebConfig(MediaStorageServiceImpl mediaStorage) {
        this.mediaStorage = mediaStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Spring chi doc duoc thu muc khi location ket thuc bang "/"; toUri() bo "/" neu thu muc chua ton tai.
        String location = mediaStorage.mediaDir().toUri().toString();
        registry.addResourceHandler(MediaStorageServiceImpl.PUBLIC_PREFIX + "**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
