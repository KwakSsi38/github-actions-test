package back.domain.prompt.prompt.parser;

import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.provider.KeywordRuleProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

import static back.domain.prompt.prompt.enums.Category.*;

@Component
@RequiredArgsConstructor
public class SkillNormalizeParser {

    private final KeywordRuleProvider provider;

    // 태그 추출
    public Set<String> extractTags(String summary, String content) {
        Set<String> tags = new LinkedHashSet<>();
        String normalized = normalize(summary, content);

        tags.addAll(extractKeywordTags(normalized));

        return tags;
    }

    // tagRules() 기반 태그 추출
    private List<String> extractKeywordTags(String text) {
        Set<String> tags = new LinkedHashSet<>();

        for (var entry : provider.getTagRules().entrySet()) {
            for (String keyword : entry.getValue()) {
                if (containsToken(text, keyword)) {
                    tags.add(entry.getKey());
                    break;
                }
            }
        }

        return new ArrayList<>(tags);
    }

    // 카테고리 추출
    public Category extractCategory(String summary, String content) {
        String normalized = normalize(summary, content);

        Map<Category, Integer> scores = new HashMap<>();

        for (var entry : provider.getCategoryRules().entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (containsToken(normalized, keyword)) {
                    score++;
                }
            }
            scores.put(entry.getKey(), score);
        }

        if (scores.get(BACKEND) > 0 && Objects.equals(scores.get(BACKEND), scores.get(FRONTEND))) {
            scores.put(FULLSTACK, scores.get(BACKEND) + 1);
        }

        return scores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(Category.OTHER);
    }

    private String normalize(String summary, String content) {
        return ((summary == null ? "" : summary) + " " + (content == null ? "" : content))
                .toLowerCase()
                // 일반 정규화
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private boolean containsToken(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }

        // keyword 앞뒤에 영어/숫자가 붙어있지 않을 때만 매칭
        String regex = "(?i)(?<![a-z0-9])"
                + Pattern.quote(keyword)
                + "(?![a-z0-9])";

        return Pattern.compile(regex).matcher(text).find();
    }


}
