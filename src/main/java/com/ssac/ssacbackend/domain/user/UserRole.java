package com.ssac.ssacbackend.domain.user;

/**
 * 사용자 권한 역할.
 *
 * <p>JWT payload의 role claim에 사용된다.
 */
public enum UserRole {
    USER, ADMIN
}
