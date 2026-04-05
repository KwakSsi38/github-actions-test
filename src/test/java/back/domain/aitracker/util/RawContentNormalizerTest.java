package back.domain.aitracker.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RawContentNormalizerTest {

    @Test
    @DisplayName("null 입력 시 null을 반환한다.")
    void normalize_NullInput_ReturnsNull() {
        assertThat(RawContentNormalizer.normalize(null)).isNull();
    }

    @Test
    @DisplayName("소셜 공유 버튼 블록을 제거한다.")
    void normalize_RemovesSocialShareBlock() {
        String input = "Some text before.\nShare\nFacebook\nTwitter\nLinkedIn\nMail\nSome text after.";
        String expected = "Some text before.\nSome text after.";
        
        assertThat(RawContentNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("코드 복사 버튼 텍스트를 제거한다.")
    void normalize_RemovesCodeCopyButtons() {
        String input = "Here is some code.\nPython\nCopied\nprint('Hello')\nGo\nCopied\nfmt.Println(\"World\")";
        String expected = "Here is some code.\nprint('Hello')\nfmt.Println(\"World\")";
        
        assertThat(RawContentNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Related Posts 마지막 등장 이후 텍스트를 제거한다.")
    void normalize_TruncatesAfterRelatedPosts() {
        String input = "Main content.\nRelated posts\nPost 1\nPost 2";
        String expected = "Main content.";
        
        assertThat(RawContentNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Keep reading 마지막 등장 이후 텍스트를 제거한다.")
    void normalize_TruncatesAfterLastKeepReading() {
        String input = "Main content.\nKeep reading for more context.\nMore content.\nKeep reading\nArticle 1\nArticle 2";
        String expected = "Main content.\nKeep reading for more context.\nMore content.";
        
        assertThat(RawContentNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("연속된 빈 줄(3줄 이상)을 2줄로 정규화하고, 앞뒤 공백을 제거한다.")
    void normalize_NormalizesExcessBlankLines() {
        String input = "  Line 1\n\n\n\nLine 2\n\n\nLine 3  ";
        String expected = "Line 1\n\nLine 2\n\nLine 3";
        
        assertThat(RawContentNormalizer.normalize(input)).isEqualTo(expected);
    }
}