package com.sprint.otboo.clothing.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String upload(MultipartFile file);

    void delete(String url);
}