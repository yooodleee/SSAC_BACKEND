package com.ssac.ssacbackend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssac.ssacbackend.domain.content.ContentCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentCategoryTest {

    @Test
    @DisplayName("유효한 Notion 태그로 ContentCategory를 찾는다")
    void fromNotionTag_유효한_태그() {
        assertThat(ContentCategory.fromNotionTag("realestate")).isEqualTo(ContentCategory.REALESTATE);
        assertThat(ContentCategory.fromNotionTag("tax")).isEqualTo(ContentCategory.TAX);
        assertThat(ContentCategory.fromNotionTag("work")).isEqualTo(ContentCategory.WORK);
        assertThat(ContentCategory.fromNotionTag("investment")).isEqualTo(ContentCategory.INVESTMENT);
        assertThat(ContentCategory.fromNotionTag("scholarship")).isEqualTo(ContentCategory.SCHOLARSHIP);
        assertThat(ContentCategory.fromNotionTag("budget")).isEqualTo(ContentCategory.SERIES);
    }

    @Test
    @DisplayName("유효하지 않은 Notion 태그는 IllegalArgumentException을 던진다")
    void fromNotionTag_유효하지_않은_태그() {
        assertThatThrownBy(() -> ContentCategory.fromNotionTag("unknown"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기존 finance 태그는 더 이상 유효하지 않다")
    void fromNotionTag_finance_유효하지_않음() {
        assertThatThrownBy(() -> ContentCategory.fromNotionTag("finance"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findById는 notionTag 기준으로 탐색한다")
    void findById_notionTag_기준() {
        assertThat(ContentCategory.findById("investment")).contains(ContentCategory.INVESTMENT);
        assertThat(ContentCategory.findById("finance")).isEmpty();
    }

    @Test
    @DisplayName("all()은 6개 카테고리를 반환한다")
    void all_카테고리_6개() {
        assertThat(ContentCategory.all()).hasSize(6);
    }

    @Test
    @DisplayName("각 카테고리의 notionTag와 label이 올바르게 정의된다")
    void 카테고리_필드_정의() {
        assertThat(ContentCategory.INVESTMENT.getNotionTag()).isEqualTo("investment");
        assertThat(ContentCategory.INVESTMENT.getLabel()).isEqualTo("재테크/신용");
        assertThat(ContentCategory.SERIES.getNotionTag()).isEqualTo("budget");
        assertThat(ContentCategory.SERIES.getLabel()).isEqualTo("시리즈");
        assertThat(ContentCategory.WORK.getNotionTag()).isEqualTo("work");
    }
}
