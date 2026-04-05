package back.domain.info.service;

import back.domain.info.dto.data.ItemDto;
import back.domain.info.dto.response.PageUpdateRequestResponse;
import back.domain.info.dto.response.UpdateRequestResponse;
import back.domain.info.dto.data.UpdateRequestDto;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;
import back.domain.info.mapper.RequestMapper;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiVendorRepository;
import back.domain.info.repository.UpdateRequestRepository;
import back.global.storage.OciObjectStorageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
)
public class UpdateRequestServiceImpl implements UpdateRequestService {

    private static final String BASE_PATH = "data/ai-tracker/updates_raw.json";

    private final ObjectMapper objectMapper;
    private final OciObjectStorageReader storageReader;
    private final AiVendorRepository aiVendorRepository;
    private final AiModelFamilyRepository familyRepository;
    private final UpdateRequestRepository requestRepository;
    private final RequestMapper requestMapper;

    @Override
    public void run() {
        String content = storageReader.readText(BASE_PATH);
        if (content != null) {
            processJson(BASE_PATH, content);
        }
    }

    private void processJson(String resourceName, String json) {
        UpdateRequestDto requestDto;
        try {
            requestDto = objectMapper.readValue(json, UpdateRequestDto.class);
        } catch (Exception e) {
            log.error("[UpdateRequestServiceImpl] JSON 파싱 실패: {}", resourceName, e);
            return;
        }

        if (requestDto == null) {
            log.warn("[UpdateRequestServiceImpl] model_benchmarks JSON 파일에서 읽은 데이터가 없습니다.");
            return;
        }

        int createdCount = 0;
        int skippedCount = 0;

        for (ItemDto dto : requestDto.getItems()) {
            AiVendor vendor = aiVendorRepository.findByName(dto.getProvider()).orElse(null);

            if (vendor == null) {
                log.warn("[UpdateRequestServiceImpl] 없는 제조사입니다.");
                skippedCount++;
                continue;
            }

            createUpdateRequest(dto, vendor);
            createdCount++;

        }

        log.info(
                "[UpdateRequestServiceImpl] UpdateRequest insert 완료. created={}, skipped={}, total={}",
                createdCount,
                skippedCount,
                createdCount + skippedCount
        );
    }

    @Transactional
    private void createUpdateRequest(ItemDto dto, AiVendor vendor) {

        AiModelFamily family = familyRepository.findByFamilyName(dto.getFamily()).orElse(null);

        UpdateRequest updateRequest = requestMapper.toUpdateRequestEntity(dto, vendor, family);

        requestRepository.save(updateRequest);

    }

    @Transactional
    public void updateStatus(Long id, String status) {

        String newStatus = status.toUpperCase();

        // Status enum 값이 유효한지 검증
        if (isValidStatus(newStatus)) {
            UpdateRequest updateRequest = requestRepository.findById(id).orElse(null);
            if (updateRequest != null) {
                updateRequest.setStatus(Status.valueOf(newStatus));
            }
        } else {
            // 유효하지 않은 상태일 경우
            throw new IllegalArgumentException("유효하지 않는 상태 값 입니다. (PENDING, APPROVED, REJECTED)");
        }
    }

    // Status enum에 해당하는 값인지 체크하는 메서드
    private boolean isValidStatus(String status) {
        try {
            Status.valueOf(status); // status가 Status enum에 있는지 확인
            return true;
        } catch (IllegalArgumentException e) {
            return false; // enum에 없으면 false 반환
        }
    }

    @Override
    @Transactional
    public PageUpdateRequestResponse getUpdates(Pageable pageable) {
        return new PageUpdateRequestResponse(requestRepository.findAll(pageable).map(UpdateRequestResponse::new));
    }

    @Override
    @Transactional
    public PageUpdateRequestResponse getUpdatesApproved(Pageable pageable) {
        return new PageUpdateRequestResponse(
                requestRepository.findAllByStatus(Status.APPROVED, pageable).map(UpdateRequestResponse::new)
        );
    }
}
