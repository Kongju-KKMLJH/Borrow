package kkmljh.borrow.common.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 공용 이미지 업로드 (공간·활동 공통). 앱에서 카메라 촬영/갤러리 선택 이미지를 먼저 여기로 올려
 * 상대 URL 목록을 받은 뒤, 그 값을 공간/활동 등록 요청의 imageUrls에 담아 보낸다.
 */
@Tag(name = "Upload", description = "이미지 업로드 (공용)")
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "이미지 업로드", description = "multipart/form-data 파트 이름 'files'로 1장 이상 업로드")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResponse> upload(@RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.ok(new UploadResponse(fileStorageService.storeAll(files)));
    }
}
