package com.ssac.ssacbackend.domain.content;

/**
 * 콘텐츠 난이도 분류.
 *
 * <p>Notion 데이터베이스의 difficulty select 속성 값과 대응한다.
 * 사용자 레벨({@link com.ssac.ssacbackend.domain.user.UserLevel})과 동일한 계층 구조를 가진다.
 */
public enum ContentDifficulty {
    SEED, SPROUT, TREE
}
