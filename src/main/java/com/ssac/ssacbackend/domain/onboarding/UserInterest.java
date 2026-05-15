package com.ssac.ssacbackend.domain.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 관심 도메인 엔티티.
 */
@Entity
@Table(name = "user_interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String domainId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserInterest(Long userId, String domainId) {
        this.userId = userId;
        this.domainId = domainId;
        this.createdAt = LocalDateTime.now();
    }
}
