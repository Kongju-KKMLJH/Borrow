package kkmljh.borrow.space.dto;

/** 개최 요청 거절 (B-10): 거절 사유(선택) */
public record RejectRequest(
        String reason
) {
}
