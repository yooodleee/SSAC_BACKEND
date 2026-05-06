package com.ssac.ssacbackend.dto;

/**
 * traceId 기반 에러 로그 조회 결과 DTO.
 */
public record ErrorLogEntry(
    String timestamp,
    String level,
    String message,
    String stackTrace
) {}
