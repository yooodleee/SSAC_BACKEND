package com.ssac.ssacbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 메뉴 클릭 이벤트 수신 요청 DTO.
 *
 * <p>userId와 guestId 중 하나는 반드시 포함되어야 한다.
 */
public record MenuClickRequest(
    @NotBlank String eventType,
    @NotBlank String menuId,
    @NotBlank String menuName,
    String userId,
    String guestId,
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime clickedAt,
    @NotBlank String pageContext
) {
    public boolean hasIdentifier() {
        return (userId != null && !userId.isBlank())
            || (guestId != null && !guestId.isBlank());
    }
}
