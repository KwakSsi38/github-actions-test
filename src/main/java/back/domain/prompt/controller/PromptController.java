package back.domain.prompt.controller;

import back.domain.prompt.service.PromptServiceImpl;
import back.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prompts")
public class PromptController {
    private final PromptServiceImpl promptServiceImpl;

    @PostMapping("/run")
    public ResponseEntity<RsData<Void>> run() throws InterruptedException {
        promptServiceImpl.run();

        return ResponseEntity.ok(new RsData<>("데이터 적재 완료"));
    }
}
