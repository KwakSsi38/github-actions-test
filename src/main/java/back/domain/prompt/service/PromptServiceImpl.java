package back.domain.prompt.service;

import back.domain.prompt.dto.PromptRepoItem;
import back.domain.prompt.dto.SkillData;
import back.domain.prompt.entity.Repository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
@Slf4j
public class PromptServiceImpl implements PromptService {

    private final SkillNormalizeServiceImpl normalizeService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
    public PromptServiceImpl(SkillNormalizeServiceImpl normalizeService, ObjectMapper objectMapper) {
        this.normalizeService = normalizeService;
        this.objectMapper = objectMapper;
    }

    @Value("${app.prompts.base-path:data/prompts}")
    private String promptsBasePath;

    @Override
    public void run() {

        File baseDir = new File(promptsBasePath);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.error("폴더가 존재하지 않습니다: {}", baseDir.getAbsolutePath());
            return;
        }

        File[] jsonFiles = baseDir.listFiles(
                (dir, name) -> name.endsWith(".json")
        );

        if (jsonFiles == null || jsonFiles.length == 0) {
            log.warn("처리할 JSON 파일이 없습니다.");
            return;
        }

        for (File jsonFile : jsonFiles) {
            try {
                processFile(jsonFile);
            } catch (Exception e) {
                log.error("파일 처리 실패: {}", jsonFile.getName(), e);
            }
        }

    }

    private void processFile(File file) {

        try {
            String json = Files.readString(file.toPath());
            PromptRepoItem repoItem = objectMapper.readValue(json, PromptRepoItem.class);

            // 누락 케이스 처리
            if (repoItem.getRepository() == null) {
                log.warn("repository 누락 스킵: {}", file.getName());
                return;
            }

            // 1. repository 저장
            Repository repository = normalizeService.normalizeRepository(repoItem);

            // 2. skills 저장
            if (repoItem.getSkills() != null && !repoItem.getSkills().isEmpty()) {
                for (SkillData skillData : repoItem.getSkills()) {
                    try {
                        normalizeService.normalizeSkill(repository, skillData);
                    } catch (Exception e) {
                        log.error("Skill 처리 실패: {}/{}",
                                repoItem.getRepository().getSourceRepo(),
                                skillData.getName(), e);
                    }
                }
            } else {
                log.warn("skills 없음: {}", repoItem.getRepository().getSourceRepo());
            }

            // 3. agent 저장
            if (repoItem.getAgent() != null) {
                try {
                    normalizeService.normalizeAgent(repository, repoItem.getAgent());
                } catch (Exception e) {
                    log.error("Agent 처리 실패: {}", repoItem.getRepository().getSourceRepo(), e);
                }
            } else {
                log.warn("agent 없음: {}", repoItem.getRepository().getSourceRepo());
            }

        } catch (IOException e) {
            log.error("JSON 파싱 실패: {}", file.getName(), e);
        }
    }

    // OCI Object Storage 연동 시 이 메서드만 교체
    private PromptRepoItem readFromOci(String bucket, String objectName) throws IOException {
        // TODO: OCI SDK 연동
        return null;
    }
}
