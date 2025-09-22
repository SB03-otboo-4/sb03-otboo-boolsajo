package com.sprint.otboo.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부파일과 같은 바이너리 파일 저장을 위한 공통 인터페이스*/
public interface FileStorageService {

    /**
     * 파일을 저장하고 접근 가능한 URL을 반환 ( 파일이 없을 시 빈 문자열 )
     * */
    String upload(MultipartFile file);

    /**
     * URL에 연결된 파일을 삭제 ( URL이 비어 있으면 아무 작업도 하지 않음 )
     * */
    void delete(String url);
}