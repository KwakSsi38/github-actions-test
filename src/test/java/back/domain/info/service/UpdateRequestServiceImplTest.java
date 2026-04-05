package back.domain.info.service;

import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;
import back.domain.info.mapper.RequestMapper;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiVendorRepository;
import back.domain.info.repository.UpdateRequestRepository;
import back.global.storage.OciObjectStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UpdateRequestServiceImplTest {

    private static final String BASE_PATH = "data/ai-tracker/updates_raw.json";

    private OciObjectStorageReader storageReader;
    private AiVendorRepository aiVendorRepository;
    private AiModelFamilyRepository familyRepository;
    private UpdateRequestRepository requestRepository;
    private UpdateRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        storageReader = mock(OciObjectStorageReader.class);
        aiVendorRepository = mock(AiVendorRepository.class);
        familyRepository = mock(AiModelFamilyRepository.class);
        requestRepository = mock(UpdateRequestRepository.class);
        service = new UpdateRequestServiceImpl(
                new ObjectMapper(),
                storageReader,
                aiVendorRepository,
                familyRepository,
                requestRepository,
                new RequestMapper()
        );
    }

    @Test
    void run_savesUpdateRequestsWhenVendorExists() {
        String json = """
                {
                  "collected_at": "2026-04-03T10:00:00+09:00",
                  "count": 1,
                  "items": [
                    {
                      "id": "item-1",
                      "provider": "OpenAI",
                      "family": "GPT",
                      "source_type": "blog",
                      "url": "https://example.com/post",
                      "summary": "summary",
                      "raw_content": "raw text"
                    }
                  ]
                }
                """;
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://openai.com")
                .isActive(true)
                .isDeprecated(false)
                .build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT")
                .commonDescription("desc")
                .build();

        when(storageReader.readText(BASE_PATH)).thenReturn(json);
        when(aiVendorRepository.findByName("OpenAI")).thenReturn(Optional.of(vendor));
        when(familyRepository.findByFamilyName("GPT")).thenReturn(Optional.of(family));
        when(requestRepository.save(any(UpdateRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.run();

        ArgumentCaptor<UpdateRequest> captor = ArgumentCaptor.forClass(UpdateRequest.class);
        verify(requestRepository).save(captor.capture());
        UpdateRequest saved = captor.getValue();
        assertThat(saved.getSourceId()).isEqualTo("item-1");
        assertThat(saved.getVendor()).isSameAs(vendor);
        assertThat(saved.getFamily()).isSameAs(family);
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        assertThat(saved.getNotifiedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void run_skipsItemsWhenVendorDoesNotExist() {
        String json = """
                {
                  "collected_at": "2026-04-03T10:00:00+09:00",
                  "count": 1,
                  "items": [
                    {
                      "id": "item-1",
                      "provider": "Unknown",
                      "family": "GPT",
                      "source_type": "blog",
                      "url": "https://example.com/post",
                      "summary": "summary",
                      "raw_content": "raw text"
                    }
                  ]
                }
                """;
        when(storageReader.readText(BASE_PATH)).thenReturn(json);
        when(aiVendorRepository.findByName("Unknown")).thenReturn(Optional.empty());

        service.run();

        verify(requestRepository, never()).save(any());
    }

    @Test
    void run_ignoresInvalidJson() {
        when(storageReader.readText(BASE_PATH)).thenReturn("{broken");

        service.run();

        verifyNoInteractions(aiVendorRepository, familyRepository, requestRepository);
    }

    @Test
    void updateStatus_updatesEntityWhenStatusIsValid() {
        UpdateRequest request = UpdateRequest.builder()
                .sourceId("item-1")
                .status(Status.PENDING)
                .build();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        service.updateStatus(1L, "approved");

        assertThat(request.getStatus()).isEqualTo(Status.APPROVED);
    }

    @Test
    void updateStatus_throwsWhenStatusIsInvalid() {
        assertThatThrownBy(() -> service.updateStatus(1L, "bad-status"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getUpdates_mapsRepositoryPage() {
        UpdateRequest request = createUpdateRequestEntity();
        when(requestRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(request), PageRequest.of(0, 10), 1));

        var response = service.getUpdates(PageRequest.of(0, 10));

        assertThat(response.getContents()).hasSize(1);
        assertThat(response.getContents().getFirst().getVendorName()).isEqualTo("OpenAI");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getUpdatesApproved_mapsApprovedPage() {
        UpdateRequest request = createUpdateRequestEntity();
        when(requestRepository.findAllByStatus(Status.APPROVED, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(request), PageRequest.of(0, 5), 1));

        var response = service.getUpdatesApproved(PageRequest.of(0, 5));

        assertThat(response.getContents()).hasSize(1);
        assertThat(response.getContents().getFirst().getFamilyName()).isEqualTo("GPT");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(5);
    }

    private UpdateRequest createUpdateRequestEntity() {
        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://openai.com")
                .isActive(true)
                .isDeprecated(false)
                .build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT")
                .commonDescription("desc")
                .build();

        UpdateRequest request = UpdateRequest.builder()
                .sourceId("item-1")
                .vendor(vendor)
                .family(family)
                .sourceUrl("https://example.com/post")
                .sourceType("blog")
                .summary("summary")
                .status(Status.APPROVED)
                .notifiedAt(LocalDate.of(2026, 4, 3))
                .reviewedAt(LocalDateTime.of(2026, 4, 3, 10, 0))
                .build();
        ReflectionTestUtils.setField(request, "id", 1L);
        ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.of(2026, 4, 3, 9, 0));
        return request;
    }
}
