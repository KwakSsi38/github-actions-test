package back.domain.prompt.prompt.service;

import back.domain.prompt.prompt.dto.AgentDto;
import back.domain.prompt.prompt.dto.PromptRepoItem;
import back.domain.prompt.prompt.dto.SkillDto;
import back.domain.prompt.prompt.entity.Repository;
import back.global.storage.OciObjectStorageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "ObjectMapper is managed by Spring and used only inside this service."
)
public class PromptServiceImpl implements PromptService {

    private static final String BASE_PATH = "data/prompts";

    private final SkillUpsertService skillUpsertService;
    private final ObjectMapper objectMapper;
    private final OciObjectStorageReader objectStorageReader;

    @Value("${app.prompts.oci-prefix:data/prompts/}")
    private String promptsOciPrefix;

    @Override
    public void run() {
        runFromOci();
    }

    private void runFromOci() {
        List<String> objectNames = objectStorageReader.listObjectNames(promptsOciPrefix).stream()
                .filter(name -> name.endsWith(".json"))
                .toList();

        if (objectNames.isEmpty()) {
            log.warn("OCI Object Storage에서 프롬프트 JSON 파일을 찾지 못했습니다. prefix={}", normalizePrefix());
            return;
        }

        for (String objectName : objectNames) {
            try {
                String content = objectStorageReader.readText(objectName);
                if (content == null) {
                    continue;
                }
                processJson(objectName, content);
            } catch (Exception e) {
                log.error("프롬프트 파일 처리 실패: {}", objectName, e);
            }
        }
    }

    private String normalizePrefix() {
        if (promptsOciPrefix == null || promptsOciPrefix.isBlank()) {
            return "";
        }
        return promptsOciPrefix.endsWith("/") ? promptsOciPrefix : promptsOciPrefix + "/";
    }

    private void processJson(String resourceName, String json) {
        try {
            PromptRepoItem repoItem = objectMapper.readValue(json, PromptRepoItem.class);

            if (repoItem.getRepository() == null) {
                log.warn("repository 섹션이 누락되었습니다: {}", resourceName);
                return;
            }

            Repository repository = skillUpsertService.upsertRepository(repoItem);
            String sourceRepo = repository.getSourceRepo();

            processSkills(repository, sourceRepo, repoItem.getSkills());
            processAgent(repository, sourceRepo, repoItem.getAgent());
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", resourceName, e);
        }
    }

    private void processSkills(Repository repository, String sourceRepo, List<SkillDto> skills) {
        if (skills == null || skills.isEmpty()) {
            log.warn("skills가 없습니다: {}", sourceRepo);
            return;
        }

        for (SkillDto skillDto : skills) {
            if (skillDto.getContentMd() == null) {
                log.warn("skill content_md가 없습니다: {}/{}", sourceRepo, skillDto.getName());
                continue;
            }

            try {
                skillUpsertService.upsertSkill(repository, skillDto);
            } catch (Exception e) {
                log.error("Skill 처리 실패: {}/{}", sourceRepo, skillDto.getName(), e);
            }
        }
    }

    private void processAgent(Repository repository, String sourceRepo, AgentDto agent) {
        if (agent == null) {
            log.warn("agent가 없습니다: {}", sourceRepo);
            return;
        }

        if (agent.getContentMd() == null) {
            log.warn("agent content_md가 없습니다: {}", sourceRepo);
            return;
        }

        try {
            skillUpsertService.upsertAgent(repository, agent);
        } catch (Exception e) {
            log.error("Agent 처리 실패: {}", sourceRepo, e);
        }
    }
}
