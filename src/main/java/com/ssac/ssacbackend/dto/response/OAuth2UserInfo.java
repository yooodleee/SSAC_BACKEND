package com.ssac.ssacbackend.dto.response;

import java.util.Map;

/**
 * OAuth2 서비스로부터 받은 사용자 정보를 통일된 인터페이스로 제공한다.
 */
public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getEmail();
    String getNickname();
}
