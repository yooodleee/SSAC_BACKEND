package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.search.SearchKeyword;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 검색 키워드 집계 레포지터리.
 */
public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {

    Optional<SearchKeyword> findByKeyword(String keyword);

    List<SearchKeyword> findTop10ByOrderBySearchCountDesc();
}
