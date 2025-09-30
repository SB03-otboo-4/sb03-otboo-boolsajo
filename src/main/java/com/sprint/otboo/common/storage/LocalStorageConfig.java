package com.sprint.otboo.common.storage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

@Configuration
@ConditionalOnProperty(prefix = "storage.s3", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalStorageConfig {

    @Bean
    @Qualifier("profileImageStorageService")
    public FileStorageService profileImageStorageService(LocalFileStorageService delegate) {
        return new DelegatingFileStorageService(delegate);
    }

    @Bean
    @Qualifier("clothingImageStorageService")
    public FileStorageService clothingImageStorageService(LocalFileStorageService delegate) {
        return new DelegatingFileStorageService(delegate);
    }

    private static final class DelegatingFileStorageService implements FileStorageService {
        private final FileStorageService delegate;

        private DelegatingFileStorageService(FileStorageService delegate) {
            this.delegate = delegate;
        }

        @Override
        public String upload(MultipartFile file) {
            return delegate.upload(file);
        }

        @Override
        public void delete(String url) {
            delegate.delete(url);
        }
    }
}
