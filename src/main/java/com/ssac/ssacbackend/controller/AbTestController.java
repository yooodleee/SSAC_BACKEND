package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.AbTestGroupResponse;
import com.ssac.ssacbackend.service.AbTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A/B 테스트 그룹 관리 엔드포인트.
 *
 * <p>동일 식별자는 항상 동일한 그룹을 반환한다.
 * 테스트 종료 후에는 채택된 그룹을 모든 사용자에게 반환한다.
 */
@Tag(name = "AB Test", description = "A/B 테스트 그룹 관리 API")
@RestController
@RequestMapping("/api/ab-test")
@RequiredArgsConstructor
public class AbTestController {

    private final AbTestService abTestService;

    @Operation(
        summary = "메뉴 A/B 테스트 그룹 조회",
        description = """
            [호출 화면] 앱 초기화 또는 메뉴 렌더링 시점
            [권한 조건] 인증 불필요 (비회원 포함).
            [특이 동작] userId와 guestId 중 하나를 쿼리 파라미터로 전달해야 한다.
            테스트 종료 후에는 채택된 메뉴 구조의 그룹을 모든 사용자에게 반환한다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "그룹 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "userId 또는 guestId 중 하나 필수")
    })
    @GetMapping("/menu")
    public ResponseEntity<ApiResponse<AbTestGroupResponse>> getMenuGroup(
        @Parameter(description = "로그인 사용자 ID") @RequestParam(required = false) String userId,
        @Parameter(description = "비회원 식별자") @RequestParam(required = false) String guestId) {
        String identifier = resolveIdentifier(userId, guestId);
        String group = abTestService.assignGroup(identifier);
        return ResponseEntity.ok(ApiResponse.success(new AbTestGroupResponse(group)));
    }

    private String resolveIdentifier(String userId, String guestId) {
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        if (guestId != null && !guestId.isBlank()) {
            return "guest:" + guestId;
        }
        throw new BusinessException(
            "userId 또는 guestId 중 하나는 반드시 필요합니다.",
            HttpStatus.BAD_REQUEST,
            "INVALID_EVENT_DATA"
        );
    }
}
