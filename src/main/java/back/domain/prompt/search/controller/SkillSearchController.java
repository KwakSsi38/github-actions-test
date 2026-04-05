package back.domain.prompt.search.controller;

import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.service.SkillSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SkillSearchController {

    private final SkillSearchService skillSearchService;

    @GetMapping("/api/skills/search")
    public SkillChunkSearchResultDto search(
            @RequestParam String query
    ) {
        return skillSearchService.search(query);
    }
}
