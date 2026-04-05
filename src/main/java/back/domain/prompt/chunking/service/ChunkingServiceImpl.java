package back.domain.prompt.chunking.service;

import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkingServiceImpl implements ChunkingService {

    private final SkillRepository skillRepository;
    private final ChunkingProcessor chunkingProcessor;

    // is_chunked = false인 skill을 모두 청킹한다.
    @Override
    public void chunk() {
        List<Skill> pending = skillRepository.findByIsChunkedFalse();
        log.info("청킹 대상 skill 수: {}", pending.size());

        int success = 0;
        for (Skill skill : pending) {
            try {
                chunkingProcessor.processOne(skill);
                success++;
            } catch (Exception e) {
                // 이 skill만 롤백되고 루프는 계속 진행
                log.error("청킹 실패 - skillId={}, name={}: {}", skill.getId(), skill.getName(), e.getMessage());
            }
        }

        log.info("청킹 완료: {}/{}", success, pending.size());
    }
}
