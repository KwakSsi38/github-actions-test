package back.domain.aitracker.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 트래커 수집 raw_content 전처리 유틸리티.
 *
 * <p>웹 스크래핑 결과물에 섞여 들어오는 UI 노이즈를 제거합니다.
 *
 * <ul>
 *   <li>소셜 공유 버튼 텍스트 제거 (Share / Facebook / Twitter / LinkedIn / Mail)</li>
 *   <li>코드 블록 복사 버튼 텍스트 제거 (예: "Python\nCopied", "Go\nCopied")</li>
 *   <li>관련 게시글 섹션 후방 삭제 (Related Posts / Keep reading 마지막 등장 이후 제거)</li>
 *   <li>연속 빈줄 정규화</li>
 * </ul>
 */
public final class RawContentNormalizer {

    // 소셜 공유 버튼 블록 (순서 고정, 대소문자 무관)
    private static final Pattern SOCIAL_SHARE_PATTERN = Pattern.compile(
            "Share\\s*\\nFacebook\\s*\\nTwitter\\s*\\nLinkedIn\\s*\\nMail\\s*\\n?",
            Pattern.CASE_INSENSITIVE
    );

    // 코드 블록 복사 버튼: "언어명\nCopied" (언어명은 단어 문자 1개 이상)
    private static final Pattern CODE_COPY_PATTERN = Pattern.compile(
            "\\w+\\nCopied\\n?"
    );

    // 관련 게시글 섹션 구분자 — 줄의 시작에서 단독으로 등장하는 경우만 매칭 (대소문자 무관)
    private static final Pattern TRAILING_SECTION_PATTERN = Pattern.compile(
            "(?im)^(Related Posts|Keep reading)\s*$"
    );

    // 연속 빈줄 (3줄 이상 → 2줄로)
    private static final Pattern EXCESS_BLANK_LINES_PATTERN = Pattern.compile(
            "\\n{3,}"
    );

    private RawContentNormalizer() {}

    /**
     * raw_content 문자열을 정규화합니다.
     *
     * @param raw 원본 raw_content 문자열
     * @return 정규화된 문자열. null 입력 시 null 반환
     */
    public static String normalize(String raw) {
        if (raw == null) return null;

        String result = raw;
        result = removePattern(result, SOCIAL_SHARE_PATTERN);
        result = removePattern(result, CODE_COPY_PATTERN);
        result = truncateAtLastOccurrence(result, TRAILING_SECTION_PATTERN);
        result = EXCESS_BLANK_LINES_PATTERN.matcher(result).replaceAll("\n\n");

        return result.strip();
    }

    /**
     * 패턴에 매칭되는 모든 구간을 제거합니다.
     */
    private static String removePattern(String text, Pattern pattern) {
        return pattern.matcher(text).replaceAll("");
    }

    /**
     * 패턴이 마지막으로 등장하는 지점부터 이후 텍스트를 모두 제거합니다. (rfind 방식)
     *
     * <p>매칭이 없으면 원문을 그대로 반환합니다.
     */
    private static String truncateAtLastOccurrence(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);

        int lastMatchStart = -1;
        while (matcher.find()) {
            lastMatchStart = matcher.start();
        }

        if (lastMatchStart == -1) return text;
        return text.substring(0, lastMatchStart).stripTrailing();
    }
}
