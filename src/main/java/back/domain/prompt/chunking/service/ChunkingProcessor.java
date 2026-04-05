package back.domain.prompt.chunking.service;

import back.domain.prompt.chunking.chunker.MarkdownChunker;
import back.domain.prompt.chunking.dto.Section;
import back.domain.prompt.chunking.entity.SkillChunk;
import back.domain.prompt.chunking.repository.SkillChunkRepository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.repository.SkillRepository;
import back.domain.prompt.search.util.VectorUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링 DI로 주입되는 공유 의존성을 서비스 내부 필드로 보관한다."
)
public class ChunkingProcessor {

    private static final String CHUNK_VERSION = "v1";
    private static final String EMBEDDING_MODEL = "BAAI/bge-m3";

    private final SkillChunkRepository skillChunkRepository;
    private final SkillRepository skillRepository;
    private final EmbeddingService embeddingService;
    private final MarkdownChunker markdownChunker;

    // skill 하나를 청킹·임베딩하고 결과를 저장한다.
    @Transactional
    public void processOne(Skill skill) {
        // 1. 기존 청크 삭제 (재청킹 시 이전 데이터 정리)
        skillChunkRepository.deleteBySkillId(skill.getId());

        // 2. 마크다운 청킹
        List<Section> sections = markdownChunker.chunkMarkdown(skill.getContentMd());

        // 3. 검색용 텍스트 구성 후 임베딩 일괄 요청
        List<String> searchTexts = sections.stream()
                .map(s -> buildSearchText(skill.getName(), s.sectionTitle(), s.text()))
                .toList();
        List<List<Float>> embeddings = embeddingService.embedBatch(searchTexts);

        // 4. SkillChunk 엔티티 생성 및 저장
        List<SkillChunk> skillChunks = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            skillChunks.add(SkillChunk.builder()
                    .skill(skill)
                    .chunkIndex(i)
                    .sectionTitle(sections.get(i).sectionTitle())
                    .searchText(searchTexts.get(i))
                    .charCount(sections.get(i).text().length())
                    .chunkVersion(CHUNK_VERSION)
                    .embeddingModel(EMBEDDING_MODEL)
                    .embedding(VectorUtils.toFloatArray(embeddings.get(i)))
                    .embeddedAt(OffsetDateTime.now())
                    .build());
        }
        skillChunkRepository.saveAll(skillChunks);

        // 5. 청킹 완료 표시 (실패 시 false 유지 → 다음 실행 때 재시도)
        skillRepository.markAsChunked(skill.getId());

    }

    // search_text에 skill명·섹션명 메타 정보를 접두사로 붙인다.
    private String buildSearchText(String name, String sectionTitle, String chunkText) {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append("[skill: ").append(name).append("] ");
        }
        if (sectionTitle != null) {
            sb.append("[section: ").append(sectionTitle).append("] ");
        }
        sb.append("\n").append(chunkText);
        return sb.toString().strip();
    }
}
