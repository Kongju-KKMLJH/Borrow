package kkmljh.borrow.common.storage;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 업로드된 이미지 파일을 로컬 디스크에 저장한다 (MVP — 배포 없이 LAN PC 서버용).
 * 저장 후 정적 서빙 경로 기준의 상대 URL(예: "/files/uuid.jpg")을 돌려준다.
 * 앱은 이 상대 경로 앞에 서버 base URL(LAN IP)을 붙여 조회한다.
 *
 * 실제 플랫폼 배포 시에는 이 서비스만 S3/Cloudinary 구현으로 교체하면 된다.
 */
@Service
public class FileStorageService {

    /** 허용 확장자 (기기 카메라/갤러리에서 나오는 일반 이미지 포맷) */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif", "heic", "heif");

    private final Path uploadDir;
    private final String urlPrefix;

    public FileStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.upload.url-prefix:/files}") String urlPrefix) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + this.uploadDir, e);
        }
    }

    /** 여러 장을 순서대로 저장하고 각 상대 URL을 리스트로 반환한다. */
    public List<String> storeAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        return files.stream().map(this::store).toList();
    }

    /** 파일 하나를 저장하고 상대 URL을 반환한다. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        String ext = resolveExtension(file);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // UUID 파일명만 사용하므로 원본 파일명 기반 경로 조작(../) 위험 없음. 그래도 경계 밖 저장 방지.
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return urlPrefix + "/" + filename;
    }

    /** 원본 파일명 확장자를 우선 쓰고, 없거나 허용 목록 밖이면 content-type으로 보정한다. */
    private String resolveExtension(MultipartFile file) {
        String original = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (original != null) {
            String lower = original.toLowerCase();
            if (ALLOWED_EXT.contains(lower)) {
                return lower.equals("jpeg") ? "jpg" : lower;
            }
        }
        String contentType = file.getContentType();
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/heic" -> "heic";
            case "image/heif" -> "heif";
            default -> "jpg"; // image/jpeg 및 기타 이미지
        };
    }
}
