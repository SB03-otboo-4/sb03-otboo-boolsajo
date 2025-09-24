//package com.sprint.otboo.clothing.storage;
//
//import com.sprint.otboo.clothing.exception.FileStorageException;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
///**
// * 로컬 파일 스토리지 서비스 구현
// *
// * <p>파일 업로드, 삭제 기능 제공
// *
// * <ul>
// *   <li>파일 업로드 시 로컬 디렉토리에 저장하고 URL 반환</li>
// *   <li>파일 삭제 시 로컬 디렉토리에서 제거</li>
// *   <li>업로드/삭제 실패 시 {@link FileStorageException} 발생</li>
// * </ul>
// */
//@Slf4j
//@Service
//public class LocalFileStorageService implements FileStorageService {
//
//    private final String uploadDir;
//    private final String baseUrl;
//
//    public LocalFileStorageService() {
//        this.uploadDir = "/uploads";
//        this.baseUrl = "http://localhost:8080/uploads";
//    }
//
//    // 테스트용 생성자
//    public LocalFileStorageService(String uploadDir, String baseUrl) {
//        this.uploadDir = uploadDir;
//        this.baseUrl = baseUrl;
//    }
//
//    /**
//     * 파일 업로드
//     *
//     * @param file 업로드할 파일
//     * @return 접근 가능한 파일 URL (파일 없으면 빈 문자열 반환)
//     * @throws FileStorageException 업로드 실패 시
//     */
//    @Override
//    public String upload(MultipartFile file) {
//        if (file == null || file.isEmpty()) {
//            log.debug("업로드할 파일이 없음");
//            return "";
//        }
//
//        try {
//            Path dirPath = ensureUploadDirExists();
//            Path filePath = saveFile(file, dirPath);
//            String url = toFileUrl(filePath);
//
//            log.info("로컬 파일 업로드 완료: {}", url);
//            return url;
//        } catch (IOException e) {
//            log.error("파일 업로드 실패", e);
//            throw new FileStorageException("파일 업로드 실패", e);
//        }
//    }
//
//    /**
//     * 파일 삭제
//     *
//     * @param url 삭제할 파일 URL
//     * @throws FileStorageException 삭제 실패 시
//     */
//    @Override
//    public void delete(String url) {
//        if (url == null || url.isBlank()) return;
//
//        try {
//            Path filePath = extractPathFromUrl(url);
//            Files.deleteIfExists(filePath);
//            log.info("로컬 파일 삭제 완료: {}", filePath);
//        } catch (IOException e) {
//            log.error("파일 삭제 실패: {}", url, e);
//            throw new FileStorageException("파일 삭제 실패: " + url, e);
//        }
//    }
//
//    // =================== 헬퍼 메서드 ===================
//
//    /**
//     * 업로드 디렉토리 존재 확인 후, 없으면 생성
//     *
//     * @return 업로드 디렉토리 Path
//     * @throws IOException 디렉토리 생성 실패 시
//     */
//    private Path ensureUploadDirExists() throws IOException {
//        Path dirPath = Paths.get(this.uploadDir);
//        if (!Files.exists(dirPath)) {
//            Files.createDirectories(dirPath);
//            log.debug("업로드 디렉토리 생성: {}", dirPath);
//        }
//        return dirPath;
//    }
//
//    /**
//     * 업로드할 파일명을 생성
//     *
//     * @param file MultipartFile
//     * @return 시스템 시간 기반 파일명
//     */
//    private Path saveFile(MultipartFile file, Path dirPath) throws IOException {
//        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
//        Path filePath = dirPath.resolve(fileName);
//        file.transferTo(filePath.toFile());
//        return filePath;
//    }
//
//    /**
//     * 파일 Path를 URL로 변환
//     *
//     * @param filePath 업로드된 파일 Path
//     * @return 접근 가능한 URL
//     */
//    private String toFileUrl(Path filePath) {
//        return this.baseUrl + "/" + filePath.getFileName().toString();
//    }
//
//    /**
//     * URL로부터 실제 파일 Path 추출
//     *
//     * @param url 파일 URL
//     * @return 실제 파일 Path
//     */
//    private Path extractPathFromUrl(String url) {
//        String fileName = url.replace(this.baseUrl + "/", "");
//        return Paths.get(this.uploadDir, fileName);
//    }
//}