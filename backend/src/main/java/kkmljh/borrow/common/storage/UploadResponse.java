package kkmljh.borrow.common.storage;

import java.util.List;

/** 업로드 결과 — 저장된 이미지들의 상대 URL 목록. 앱은 이 값을 등록 요청의 imageUrls로 그대로 전달한다. */
public record UploadResponse(List<String> urls) {
}
