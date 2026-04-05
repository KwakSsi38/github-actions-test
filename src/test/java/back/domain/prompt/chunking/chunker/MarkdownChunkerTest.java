package back.domain.prompt.chunking.chunker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.chunking.dto.Section;

class MarkdownChunkerTest {

    private final MarkdownChunker markdownChunker = new MarkdownChunker();

    @Test
    @DisplayName("공백 정규화 시 줄바꿈과 탭을 정리하고 앞뒤 공백을 제거한다")
    void normalizeWhitespace_normalizesLineBreaksAndTabs() {
        String normalized = markdownChunker.normalizeWhitespace("  alpha\r\n\r\n\r\nbeta\t\tgamma\rdelta  ");

        assertThat(normalized).isEqualTo("alpha\n\nbeta gamma\ndelta");
    }

    @Test
    @DisplayName("헤딩이 없으면 제목 없는 단일 섹션으로 반환한다")
    void markdownToSections_returnsSingleUntitledSectionWhenHeadingMissing() {
        List<Section> sections = markdownChunker.markdownToSections("plain text\n\nmore text");

        assertThat(sections).containsExactly(new Section(null, "plain text\n\nmore text"));
    }

    @Test
    @DisplayName("마크다운 헤딩 기준으로 섹션을 분리하고 헤딩 줄은 본문에 유지한다")
    void markdownToSections_splitsByHeadingAndKeepsHeadingLine() {
        List<Section> sections = markdownChunker.markdownToSections("""
                # Intro
                intro body
                ## Install
                install body
                ### Usage
                usage body
                """);

        assertThat(sections).hasSize(3);
        assertThat(sections.get(0)).isEqualTo(new Section("Intro", "# Intro\nintro body"));
        assertThat(sections.get(1)).isEqualTo(new Section("Install", "## Install\ninstall body"));
        assertThat(sections.get(2)).isEqualTo(new Section("Usage", "### Usage\nusage body"));
    }

    @Test
    @DisplayName("큰 블록 분할 시 overlap을 두고 최소 길이보다 짧은 마지막 조각은 버린다")
    void splitLargeBlock_overlapsAndSkipsShortTrailingPiece() {
        String text = "A".repeat(55);

        List<String> chunks = markdownChunker.splitLargeBlock(text, 30, 5, 20);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(30);
        assertThat(chunks.get(1)).hasSize(30);
        assertThat(chunks.get(1)).startsWith("A".repeat(5));
    }

    @Test
    @DisplayName("섹션이 너무 길면 여러 청크로 쪼개더라도 섹션 제목은 유지한다")
    void chunkMarkdown_splitsLongSectionAndPreservesSectionTitle() {
        String markdown = "# Install%n%s%n".formatted("A".repeat(80));
        List<Section> chunks = markdownChunker.chunkMarkdown(markdown, 40, 5, 10);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting(Section::sectionTitle).containsOnly("Install");
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text().length()).isLessThanOrEqualTo(40));
    }
}
