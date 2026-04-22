package com.ssac.ssacbackend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 퀴즈 제출 요청 DTO.
 *
 * <p>사용자가 선택한 답안 목록을 담는다. 서버에서 정답 여부를 검증한다.
 */
public record QuizSubmitRequest(

    @Schema(description = "응시할 퀴즈 ID", example = "1")
    @NotNull(message = "퀴즈 ID는 필수입니다.")
    Long quizId,

    @Schema(description = "문항별 답안 목록")
    @NotEmpty(message = "답안 목록은 비어 있을 수 없습니다.")
    @Valid
    List<AnswerItem> answers

) {

    /**
     * 문항 하나에 대한 답안.
     */
    public record AnswerItem(

        @Schema(description = "문항 ID", example = "1")
        @NotNull(message = "문항 ID는 필수입니다.")
        Long questionId,

        @Schema(description = "선택한 답안", example = "A")
        @NotNull(message = "선택한 답안은 필수입니다.")
        String selectedAnswer

    ) {
    }
}
