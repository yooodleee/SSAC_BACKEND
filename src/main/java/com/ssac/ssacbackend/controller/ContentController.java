package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.common.response.ApiResponse;
import com.ssac.ssacbackend.dto.response.ContentCompleteResponse;
import com.ssac.ssacbackend.dto.response.ContentDetailResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 콘텐츠 API 엔드포인트.
 */
@Slf4j
@Tag(name = "Content", description = "콘텐츠 목록 조회 및 학습 완료 API")
@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @Operation(
        summary = "콘텐츠 목록 조회",
        description = """
            [호출 화면] 콘텐츠 목록 화면.
            [권한 조건] 비로그인 사용자도 접근 가능. 로그인 시 완료 여부 포함.
            [필터] category, difficulty(SEED/SPROUT/TREE), domain 조합 가능.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "콘텐츠 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ContentListResponse>> getContents(
        Authentication authentication,
        @Parameter(description = "카테고리 필터. 복수 선택 시 콤마로 구분. 예) realestate,tax")
        @RequestParam(required = false) String category,
        @Parameter(description = "난이도 필터 (SEED/SPROUT/TREE)")
        @RequestParam(required = false) String difficulty,
        @Parameter(description = "도메인 필터")
        @RequestParam(required = false) String domain) {
        ContentListResponse response =
            contentService.getContents(authentication, category, difficulty, domain);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "콘텐츠 상세 조회",
        description = """
            [호출 화면] 콘텐츠 상세 화면.
            [권한 조건] 비로그인 사용자도 접근 가능.
            [특이 동작] Notion 블록을 실시간으로 조회하여 반환한다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "콘텐츠 상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "CONTENT-001: 존재하지 않는 콘텐츠")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentDetailResponse>> getContent(
        Authentication authentication,
        @Parameter(description = "콘텐츠 ID", example = "1")
        @PathVariable Long id) {
        ContentDetailResponse response = contentService.getContent(id, authentication);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "콘텐츠 학습 완료",
        description = """
            [호출 화면] 콘텐츠 학습 완료 버튼 클릭 시 호출.
            [권한 조건] 로그인 회원 전용 (USER, ADMIN).
            [특이 동작] 완료 처리 후 레벨업 조건을 자동 검사한다.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "콘텐츠 완료 처리 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "콘텐츠를 찾을 수 없음")
    })
    @PostMapping("/{contentId}/complete")
    public ResponseEntity<ApiResponse<ContentCompleteResponse>> complete(
        Authentication authentication,
        @Parameter(description = "콘텐츠 ID", example = "1")
        @PathVariable Long contentId) {
        log.debug("콘텐츠 완료 처리: email={}, contentId={}",
            authentication.getName(), contentId);
        ContentCompleteResponse response =
            contentService.complete(authentication.getName(), contentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "콘텐츠 조회 이력 기록",
        description = """
            [호출 화면] 콘텐츠 상세 진입 시 호출.
            [권한 조건] 로그인 회원 전용.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204", description = "이력 저장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "비로그인 사용자")
    })
    @PostMapping("/{contentId}/view")
    public ResponseEntity<Void> recordView(
        Authentication authentication,
        @PathVariable Long contentId) {
        contentService.recordView(authentication.getName(), contentId);
        return ResponseEntity.noContent().build();
    }
}
