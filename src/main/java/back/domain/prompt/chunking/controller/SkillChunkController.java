package back.domain.prompt.chunking.controller;

import back.domain.prompt.chunking.service.ChunkingService;
import back.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/skills")
public class SkillChunkController {

    private final ChunkingService promptService;

    @PostMapping("/chunk")
    public ResponseEntity<RsData<Void>> run() throws IOException {
        promptService.chunk();

        return ResponseEntity.ok(new RsData<>("skill_chunk 적재 완료"));
    }
}
