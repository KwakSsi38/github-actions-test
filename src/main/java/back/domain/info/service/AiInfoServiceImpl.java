package back.domain.info.service;

import back.domain.info.dto.data.FamilyDto;
import back.domain.info.dto.data.VendorDto;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.mapper.AiModelMapper;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiVendorRepository;
import back.global.storage.OciObjectStorageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
)
public class AiInfoServiceImpl implements AiInfoService {

    private final AiVendorRepository aiVendorRepository;
    private final AiModelFamilyRepository aiModelFamilyRepository;
    private final AiModelMapper aiModelMapper;
    private final ObjectMapper objectMapper;
    private final OciObjectStorageReader storageReader;

    private static final String BASE_PATH = "data/ai-info/integrated_major_models.json";

    @Override
    public void run() {
        String content = storageReader.readText(BASE_PATH);
        if (content != null) {
            processJson(BASE_PATH, content);
        }
    }

    /**
     * JSON 문자열을 파싱해 vendor·family를 upsert한다.
     */
    public void processJson(String resourceName, String json) {
        List<VendorDto> vendorDtos;
        try {
            vendorDtos = objectMapper.readValue(json, new TypeReference<List<VendorDto>>() {});
        } catch (Exception e) {
            log.error("[AiInfoService] JSON 파싱 실패: {}", resourceName, e);
            return;
        }

        if (vendorDtos == null || vendorDtos.isEmpty()) {
            log.warn("[AiInfoService] JSON 파일에서 읽은 데이터가 없습니다: {}", resourceName);
            return;
        }

        int createdVendorCount = 0;
        int updatedVendorCount = 0;

        for (VendorDto vendorDto : vendorDtos) {
            AiVendor vendor = aiVendorRepository.findByName(vendorDto.getName()).orElse(null);

            if (vendor == null) {
                vendor = createVendor(vendorDto);
                createdVendorCount++;
            } else {
                updateVendor(vendor, vendorDto);
                updatedVendorCount++;
            }

            processFamilies(vendor, vendorDto);
        }

        log.info(
                "[AiInfoService] vendor upsert 완료 ({}). created={}, updated={}, total={}",
                resourceName,
                createdVendorCount,
                updatedVendorCount,
                vendorDtos.size()
        );
    }

    @Transactional
    private AiVendor createVendor(VendorDto vendorDto) {
        AiVendor vendor = aiModelMapper.toVendorEntity(vendorDto);
        AiVendor savedVendor = aiVendorRepository.save(vendor);
        return savedVendor;
    }

    @Transactional
    private void updateVendor(AiVendor vendor, VendorDto vendorDto) {
        vendor.update(
                vendorDto.getOfficialUrl(),
                vendorDto.getIsActive(),
                vendorDto.getIsDeprecated()
        );
        log.info("[AiInfoService] vendor 수정: {}", vendor.getName());
    }

    private void processFamilies(AiVendor vendor, VendorDto vendorDto) {
        if (vendorDto.getFamilies() == null || vendorDto.getFamilies().isEmpty()) {
            return;
        }

        for (FamilyDto familyDto : vendorDto.getFamilies()) {
            AiModelFamily family = findFamily(vendor, familyDto.getFamilyName());

            if (family == null) {
                family = createFamily(vendor, familyDto);
            } else {
                updateFamily(family, familyDto);
            }

        }
    }

    private AiModelFamily findFamily(AiVendor vendor, String familyName) {
        return vendor.getModelFamilies().stream()
                .filter(family -> familyName.equals(family.getFamilyName()))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    private AiModelFamily createFamily(AiVendor vendor, FamilyDto familyDto) {
        AiModelFamily family = aiModelMapper.toFamilyEntity(familyDto, vendor);
        return aiModelFamilyRepository.save(family);
    }

    @Transactional
    private void updateFamily(AiModelFamily family, FamilyDto familyDto) {
        family.update(familyDto.getCommonDescription());
    }

}
