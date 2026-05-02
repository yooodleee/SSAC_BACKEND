package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.dto.request.MenuClickRequest;
import com.ssac.ssacbackend.service.MenuClickEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메뉴 클릭 이벤트 수집 엔드포인트.
 *
 * <p>이벤트 저장은 비동기로 처리되어 FE 응답에 영향을 주지 않는다.
 * 수신 성공 시 204 No Content를 즉시 반환한다.
 */
@Slf4j
@Tag(name = "Events", description = "사용자 행동 이벤트 수집 API")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class MenuClickEventController {

    private final MenuClickEventService menuClickEventService;

    @Operation(
        summary = "메뉴 클릭 이벤트 수집",
        description = """
            [호출 화면] 전체 페이지 (메뉴 클릭 시)
            [권한 조건] 인증 불필요 (비회원 포함).
            [특이 동작] userId와 guestId 중 하나는 반드시 포함되어야 한다.
            이벤트 저장은 비동기로 처리되며, 수신 성공 즉시 204를 반환한다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204", description = "이벤트 수신 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "유효하지 않은 이벤트 데이터")
    })
    @PostMapping("/menu-click")
    public ResponseEntity<Void> recordMenuClick(@RequestBody @Valid MenuClickRequest request) {
        if (!request.hasIdentifier()) {
            throw new BadRequestException(ErrorCode.INVALID_EVENT_DATA);
        }
        menuClickEventService.saveAsync(request);
        return ResponseEntity.noContent().build();
    }
}
