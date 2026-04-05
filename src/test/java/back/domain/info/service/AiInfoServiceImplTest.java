package back.domain.info.service;

import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.mapper.AiModelMapper;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiVendorRepository;
import back.global.storage.OciObjectStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiInfoServiceImplTest {

    private static final String BASE_PATH = "data/ai-info/integrated_major_models.json";

    private AiVendorRepository aiVendorRepository;
    private AiModelFamilyRepository aiModelFamilyRepository;
    private OciObjectStorageReader storageReader;
    private AiInfoServiceImpl service;

    @BeforeEach
    void setUp() {
        aiVendorRepository = mock(AiVendorRepository.class);
        aiModelFamilyRepository = mock(AiModelFamilyRepository.class);
        storageReader = mock(OciObjectStorageReader.class);
        service = new AiInfoServiceImpl(
                aiVendorRepository,
                aiModelFamilyRepository,
                new AiModelMapper(),
                new ObjectMapper(),
                storageReader
        );
    }

    @Test
    void run_createsNewVendorFromOciJson() {
        String json = """
                [
                  {
                    "name": "OpenAI",
                    "official_url": "https://openai.com",
                    "is_active": true,
                    "is_deprecated": false,
                    "families": [
                      {
                        "family_name": "GPT-4.1",
                        "common_description": "Flagship family"
                      }
                    ]
                  }
                ]
                """;
        when(storageReader.readText(BASE_PATH)).thenReturn(json);
        when(aiVendorRepository.findByName("OpenAI")).thenReturn(Optional.empty());
        when(aiVendorRepository.save(any(AiVendor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.run();

        ArgumentCaptor<AiVendor> vendorCaptor = ArgumentCaptor.forClass(AiVendor.class);
        verify(aiVendorRepository).save(vendorCaptor.capture());
        AiVendor savedVendor = vendorCaptor.getValue();
        assertThat(savedVendor.getName()).isEqualTo("OpenAI");
        assertThat(savedVendor.getModelFamilies()).hasSize(1);
        assertThat(savedVendor.getModelFamilies().getFirst().getFamilyName()).isEqualTo("GPT-4.1");
        verify(aiModelFamilyRepository, never()).save(any(AiModelFamily.class));
    }

    @Test
    void processJson_updatesExistingVendorAndExistingFamily() {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://old.example.com")
                .isActive(false)
                .isDeprecated(true)
                .modelFamilies(new ArrayList<>())
                .build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT-4.1")
                .commonDescription("Old family")
                .build();
        vendor.getModelFamilies().add(family);

        String json = """
                [
                  {
                    "name": "OpenAI",
                    "official_url": "https://openai.com",
                    "is_active": true,
                    "is_deprecated": false,
                    "families": [
                      {
                        "family_name": "GPT-4.1",
                        "common_description": "Updated family"
                      }
                    ]
                  }
                ]
                """;
        when(aiVendorRepository.findByName("OpenAI")).thenReturn(Optional.of(vendor));

        service.processJson("ai-info.json", json);

        assertThat(vendor.getOfficialUrl()).isEqualTo("https://openai.com");
        assertThat(vendor.getIsActive()).isTrue();
        assertThat(vendor.getIsDeprecated()).isFalse();
        assertThat(family.getCommonDescription()).isEqualTo("Updated family");
        verify(aiVendorRepository, never()).save(any());
        verify(aiModelFamilyRepository, never()).save(any());
    }

    @Test
    void processJson_createsMissingFamilyForExistingVendor() {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://openai.com")
                .isActive(true)
                .isDeprecated(false)
                .modelFamilies(new ArrayList<>())
                .build();
        String json = """
                [
                  {
                    "name": "OpenAI",
                    "official_url": "https://openai.com",
                    "is_active": true,
                    "is_deprecated": false,
                    "families": [
                      {
                        "family_name": "GPT-4.5",
                        "common_description": "New family"
                      }
                    ]
                  }
                ]
                """;
        when(aiVendorRepository.findByName("OpenAI")).thenReturn(Optional.of(vendor));
        when(aiModelFamilyRepository.save(any(AiModelFamily.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processJson("ai-info.json", json);

        ArgumentCaptor<AiModelFamily> familyCaptor = ArgumentCaptor.forClass(AiModelFamily.class);
        verify(aiModelFamilyRepository).save(familyCaptor.capture());
        assertThat(familyCaptor.getValue().getVendor()).isSameAs(vendor);
        assertThat(familyCaptor.getValue().getFamilyName()).isEqualTo("GPT-4.5");
        assertThat(familyCaptor.getValue().getCommonDescription()).isEqualTo("New family");
    }

    @Test
    void processJson_ignoresInvalidJson() {
        service.processJson("broken.json", "{not-json}");

        verifyNoInteractions(aiVendorRepository, aiModelFamilyRepository);
    }
}
