package back.domain.prompt.prompt.controller;

import back.domain.prompt.prompt.service.PromptService;
import back.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prompts")
public class PromptController {
    private final PromptService promptService;

    @PostMapping("/run")
    public ResponseEntity<RsData<Void>> run() {
        promptService.run();

        return ResponseEntity.ok(new RsData<>("데이터 적재 완료"));
    }
}
