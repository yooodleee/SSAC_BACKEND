package com.ssac.ssacbackend.domain.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 에러 로그 영속 엔티티.
 *
 * <p>WARN 레벨: 7일 보존 / ERROR 레벨: 30일 보존.
 * 보존 기간 초과 시 {@link com.ssac.ssacbackend.service.ErrorLogBatchService}가 자동 삭제한다.
 */
@Entity
@Table(name = "error_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String traceId;

    /** WARN 또는 ERROR */
    @Column(nullable = false, length = 10)
    private String level;

    @Column(nullable = false, length = 20)
    private String errorCode;

    @Column(length = 10)
    private String method;

    @Column(length = 255)
    private String path;

    @Column(length = 255)
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** ERROR 레벨에만 저장되는 스택 트레이스 */
    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ErrorLog(String traceId, String level, String errorCode,
                    String method, String path, String userId,
                    String message, String stackTrace) {
        this.traceId = traceId;
        this.level = level;
        this.errorCode = errorCode;
        this.method = method;
        this.path = path;
        this.userId = userId;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
