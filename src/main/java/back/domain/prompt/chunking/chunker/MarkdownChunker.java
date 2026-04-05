package back.domain.prompt.chunking.chunker;

import back.domain.prompt.chunking.dto.Section;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownChunker {

    // ===== 기본 설정 =====
    // 청크 하나의 최대 글자 수. 영어 기준 ~300토큰, 한국어 기준 ~800토큰에 해당
    private static final int MAX_CHARS = 1200;
    // 인접 청크 간 겹치는 글자 수. MAX_CHARS의 약 10~15% 수준으로 문맥 연속성 확보
    private static final int OVERLAP_CHARS = 150;
    // 청크로 저장할 최소 글자 수. 이보다 짧으면 의미 없는 단편으로 간주해 버림
    private static final int MIN_CHARS = 200;

    /**
     * 자연스러운 텍스트 경계를 기준으로 청크를 자르기 위한 구분자 우선순위 목록.
     * 앞에 올수록 의미 단위가 크고 선호도가 높음:
     *   1. ## / ### 헤딩 → 섹션 경계
     *   2. - (리스트 항목 시작)
     *   3. ". " (문장 끝)
     *   4. \n (줄 바꿈)
     */
    private static final List<String> CUT_DELIMITERS =
            List.of("\n## ", "\n### ", "\n- ", ". ", "\n");

    /**
     * 자연스러운 컷 포인트를 사용할 최소 위치 비율 (0.5 = 청크 중간 이후).
     * 너무 앞쪽에서 잘리면 청크가 지나치게 짧아지므로, 절반 이후에서만 허용.
     */
    private static final double MIN_CUT_RATIO = 0.5;

    /**
     * 마크다운 문자열을 기본 설정값으로 청킹해 섹션 목록을 반환한다.
     * 내부적으로 {@link #chunkMarkdown(String, int, int, int)}를 호출한다.
     */
    public List<Section> chunkMarkdown(String markdown) {
        return chunkMarkdown(markdown, MAX_CHARS, OVERLAP_CHARS, MIN_CHARS);
    }

    /**
     * 텍스트의 줄 끝 문자, 연속 빈 줄, 탭·폼피드를 정규화한다.
     *
     * CRLF / CR → LF
     * 3개 이상 연속 개행 → 2개로 압축 (단락 구분은 유지)
     * 탭·폼피드 → 공백 1개
     *
     */
    String normalizeWhitespace(String text) {
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        text = text.replaceAll("\n{3,}", "\n\n");
        text = text.replaceAll("[\t\\f]+", " ");
        return text.strip();
    }

    /**
     * maxChars를 초과하는 텍스트 블록을 슬라이딩 윈도우 방식으로 분할한다.
     *
     * 동작 방식:
     *
     * 현재 윈도우 끝({@code end})을 {@code start + maxChars}로 설정
     * 텍스트가 남아 있으면 {@code CUT_DELIMITERS} 중 가장 뒤쪽에 위치한 구분자를
     * 컷 포인트로 선택 (단, 청크 중간 이후에 있을 때만 적용)
     * 다음 윈도우 시작을 {@code end - overlapChars}로 설정해 문맥 중첩 확보
     *
     */
    List<String> splitLargeBlock(String text, int maxChars, int overlapChars, int minChars) {
        text = text.strip();
        if (text.isEmpty()) {
            return List.of();
        }
        if (text.length() <= maxChars) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int textLen = text.length();

        while (start < textLen) {
            int end = Math.min(start + maxChars, textLen);
            String piece = text.substring(start, end);

            if (end < textLen) {
                // 청크 중간(MIN_CUT_RATIO) 이후에 자연스러운 구분자가 있으면 그 위치에서 자름
                int minCutPosition = (int) (maxChars * MIN_CUT_RATIO);
                int bestCut = -1;
                for (String delimiter : CUT_DELIMITERS) {
                    int idx = piece.lastIndexOf(delimiter);
                    if (idx > bestCut) {
                        bestCut = idx;
                    }
                }
                if (bestCut > minCutPosition) {
                    end = start + bestCut;
                    piece = text.substring(start, end);
                }
            }

            piece = piece.strip();
            // 첫 번째 청크는 minChars 미만이어도 버리지 않음 (전체 텍스트가 짧은 경우 대비)
            if (!piece.isEmpty() && (piece.length() >= minChars || chunks.isEmpty())) {
                chunks.add(piece);
            }

            if (end >= textLen) {
                break;
            }

            // 다음 윈도우 시작: overlapChars만큼 겹치되, 무한 루프 방지를 위해 최소 1 전진
            int nextStart = Math.max(end - overlapChars, start + 1);
            start = nextStart;
        }

        return chunks;
    }

    /**
     * 마크다운 헤딩(#~######)을 기준으로 텍스트를 섹션 목록으로 분리한다.
     *
     * 헤딩이 없는 문서는 제목 없는 단일 섹션({@code sectionTitle = null})으로 반환된다.
     * 헤딩 줄 자체도 해당 섹션의 본문({@code body})에 포함된다.
     *
     */
    List<Section> markdownToSections(String markdown) {
        markdown = normalizeWhitespace(markdown);
        if (markdown.isEmpty()) {
            return List.of();
        }

        String[] lines = markdown.split("\n");
        List<Section> sections = new ArrayList<>();

        // #{1,6} : 마크다운 표준 헤딩 레벨 1~6
        Pattern headingPattern = Pattern.compile("^(#{1,6})\\s+(.*)$");

        String currentTitle = null;
        List<String> currentLines = new ArrayList<>();

        for (String line : lines) {
            Matcher matcher = headingPattern.matcher(line.strip());

            if (matcher.matches()) {
                // 헤딩을 만나면 이전까지 누적된 줄을 하나의 섹션으로 확정
                if (!currentLines.isEmpty()) {
                    String body = String.join("\n", currentLines).strip();
                    if (!body.isEmpty()) {
                        sections.add(new Section(currentTitle, body));
                    }
                }

                // 새 섹션 시작: 헤딩 텍스트를 제목으로, 헤딩 줄을 본문 첫 줄로
                currentTitle = matcher.group(2).strip();
                currentLines = new ArrayList<>();
                currentLines.add(line);
            } else {
                currentLines.add(line);
            }
        }

        // 루프 종료 후 마지막 섹션 저장
        if (!currentLines.isEmpty()) {
            String body = String.join("\n", currentLines).strip();
            if (!body.isEmpty()) {
                sections.add(new Section(currentTitle, body));
            }
        }

        // 헤딩이 전혀 없는 문서는 전체를 하나의 무제목 섹션으로 처리
        if (sections.isEmpty()) {
            return List.of(new Section(null, markdown));
        }

        return sections;
    }

    /**
     * 마크다운을 섹션 단위로 나눈 뒤, 각 섹션이 maxChars를 초과하면 추가 분할한다.
     * 분할된 조각에는 원본 섹션 제목이 그대로 유지된다.
     */
    List<Section> chunkMarkdown(
            String markdown,
            int maxChars,
            int overlapChars,
            int minChars
    ) {
        List<Section> sections = markdownToSections(markdown);
        List<Section> finalChunks = new ArrayList<>();

        for (Section section : sections) {
            List<String> pieces = splitLargeBlock(
                    section.text(), maxChars, overlapChars, minChars);

            for (String piece : pieces) {
                finalChunks.add(new Section(
                        section.sectionTitle(),
                        piece
                ));
            }
        }

        return finalChunks;
    }
}
