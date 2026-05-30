package com.ssac.ssacbackend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.news.News;
import com.ssac.ssacbackend.domain.news.NewsView;
import com.ssac.ssacbackend.repository.NewsViewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MysqlViewCountStoreTest {

    @Mock
    private NewsViewRepository newsViewRepository;

    @Mock
    private News news;

    @InjectMocks
    private MysqlViewCountStore mysqlViewCountStore;

    @Test
    @DisplayName("record - news_views 테이블에 NewsView를 저장한다")
    void record_정상() {
        mysqlViewCountStore.record(news);

        verify(newsViewRepository).save(any(NewsView.class));
    }
}
