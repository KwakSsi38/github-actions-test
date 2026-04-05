package back.domain.prompt.prompt.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.provider.KeywordRuleProvider;

class SkillNormalizeParserTest {

    private final SkillNormalizeParser parser = new SkillNormalizeParser(new KeywordRuleProvider());

    @Test
    @DisplayName("summary와 content에서 키워드 기반 태그를 추출한다")
    void extractTags_returnsMatchedTags() {
        Set<String> tags = parser.extractTags(
                "Spring Boot backend with PostgreSQL",
                "This skill configures Docker, JWT auth, and Java service code."
        );

        assertThat(tags).contains("spring", "postgres", "docker", "jwt", "java");
    }

    @Test
    @DisplayName("backend와 frontend 점수가 같으면 fullstack으로 분류한다")
    void extractCategory_returnsFullstackWhenBackendAndFrontendTie() {
        Category category = parser.extractCategory(
                "react spring",
                "plain text"
        );

        assertThat(category).isEqualTo(Category.FULLSTACK);
    }

    @Test
    @DisplayName("일치하는 키워드가 없으면 OTHER를 반환한다")
    void extractCategory_returnsOtherWhenNoKeywordMatched() {
        Category category = parser.extractCategory(
                null,
                null
        );

        assertThat(category).isEqualTo(Category.OTHER);
    }
}
