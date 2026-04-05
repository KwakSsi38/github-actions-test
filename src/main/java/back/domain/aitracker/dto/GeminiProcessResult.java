package back.domain.aitracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Gemini API 단일 호출 응답 파싱용 내부 DTO.
 *
 * <p>아이템당 1회 호출로 번역·요약·family 매칭·날짜 추출을 일괄 처리합니다.
 */
public record GeminiProcessResult(
        String title,
        String summary,
        @JsonProperty("family_name") String familyName,
        @JsonProperty("notified_at") String notifiedAt) {}
