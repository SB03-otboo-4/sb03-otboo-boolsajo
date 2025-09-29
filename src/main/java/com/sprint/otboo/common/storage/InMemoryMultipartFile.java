package com.sprint.otboo.common.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public class InMemoryMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;

    public InMemoryMultipartFile(String originalFilename, String contentType, byte[] content) {
        this.name = "image";
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.content = content != null ? content : new byte[0];
    }

    @Override public String getName() { return name; }
    @Override public String getOriginalFilename() { return originalFilename; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() { return content.length == 0; }
    @Override public long getSize() { return content.length; }
    @Override public byte[] getBytes() { return content.clone(); }
    @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }

    @Override
    public void transferTo(File dest) throws IOException {
        if (!dest.exists() && !dest.createNewFile()) {
            throw new IOException("파일을 생성할 수 없습니다: " + dest);
        }
        try (var out = java.nio.file.Files.newOutputStream(dest.toPath())) {
            out.write(content);
        }
    }
}