package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.ContentListResponse;
import com.ssac.ssacbackend.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 콘텐츠 목록 조회 API 엔드포인트.
 */
@Slf4j
@Tag(name = "Content", description = "콘텐츠 목록 조회 API")
@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @Operation(
        summary = "레벨/카테고리 콘텐츠 목록 조회",
        description = """
            [호출 화면] 홈 카테고리 클릭 또는 레벨별 콘텐츠 목록 화면.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] level 미지정 시 사용자의 현재 레벨 기준으로 필터링.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "콘텐츠 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ContentListResponse>> getContents(
        Authentication authentication,
        @Parameter(description = "레벨 필터 (SEED/SPROUT/TREE). 미지정 시 사용자 레벨 적용.")
        @RequestParam(required = false) String level,
        @Parameter(description = "카테고리 필터 (realestate/tax/finance/scholarship)")
        @RequestParam(required = false) String category) {
        log.debug("콘텐츠 목록 조회: email={}, level={}, category={}",
            authentication.getName(), level, category);
        ContentListResponse response = contentService.getContents(
            authentication.getName(), level, category);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
