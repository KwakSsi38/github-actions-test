package back.domain.aimodel.service;

import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse;
import back.domain.aimodel.dto.integrated.BenchmarkRecord;
import back.domain.aimodel.dto.integrated.IntegratedVendor;
import back.domain.aimodel.dto.openrouter.OrModelsResponse;
import back.global.exception.ServiceException;
import back.global.infra.oci.OciStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiDataPipelineServiceTest {

    @InjectMocks AiDataPipelineServiceImpl pipelineService;

    @Mock OciStorageService  ociStorageService;
    @Mock ModelMergeService  modelMergeService;
    @Mock BenchmarkService   benchmarkService;
    @Mock DescriptionService descriptionService;

    @Test
    @DisplayName("정상 실행 시 5단계가 순서대로 호출된다")
    void run_success_callsAllStepsInOrder() {
        OrModelsResponse orResponse = new OrModelsResponse(List.of());
        AaModelsResponse aaResponse = new AaModelsResponse(List.of());
        List<IntegratedVendor> integrated = List.of();
        List<IntegratedVendor> withDesc   = List.of();
        List<BenchmarkRecord>  benchmarks = List.of();

        when(ociStorageService.downloadJson(contains("models_info_raw"),      eq(OrModelsResponse.class))).thenReturn(orResponse);
        when(ociStorageService.downloadJson(contains("models_benchmark_raw"), eq(AaModelsResponse.class))).thenReturn(aaResponse);
        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(modelMergeService.merge(orResponse)).thenReturn(integrated);
        when(descriptionService.generateAndApply(integrated)).thenReturn(withDesc);
        when(benchmarkService.extract(aaResponse)).thenReturn(benchmarks);

        pipelineService.run();

        var inOrder = inOrder(ociStorageService, modelMergeService, descriptionService, benchmarkService);
        inOrder.verify(ociStorageService).downloadJson(contains("models_info_raw"),      eq(OrModelsResponse.class));
        inOrder.verify(ociStorageService).downloadJson(contains("models_benchmark_raw"), eq(AaModelsResponse.class));
        inOrder.verify(modelMergeService).merge(orResponse);
        inOrder.verify(descriptionService).generateAndApply(integrated);
        inOrder.verify(ociStorageService).uploadJson(contains("integrated"), eq(withDesc));
        inOrder.verify(benchmarkService).extract(aaResponse);
        inOrder.verify(ociStorageService).uploadJson(contains("benchmarks"), eq(benchmarks));
    }

    @Test
    @DisplayName("description 생성 실패 시 빈값으로 계속 진행한다")
    void run_descriptionFails_continuesWithEmptyDescription() {
        OrModelsResponse orResponse = new OrModelsResponse(List.of());
        AaModelsResponse aaResponse = new AaModelsResponse(List.of());
        List<IntegratedVendor> integrated = List.of();

        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(ociStorageService.downloadJson(contains("models_info_raw"),      eq(OrModelsResponse.class))).thenReturn(orResponse);
        when(ociStorageService.downloadJson(contains("models_benchmark_raw"), eq(AaModelsResponse.class))).thenReturn(aaResponse);
        when(modelMergeService.merge(orResponse)).thenReturn(integrated);
        when(descriptionService.generateAndApply(integrated)).thenThrow(new RuntimeException("Gemini 실패"));
        when(benchmarkService.extract(aaResponse)).thenReturn(List.of());

        assertThatCode(() -> pipelineService.run()).doesNotThrowAnyException();

        // description 실패해도 integrated 업로드는 호출돼야 한다
        verify(ociStorageService).uploadJson(contains("integrated"), eq(integrated));
    }

    @Test
    @DisplayName("OCI 다운로드 실패 시 ServiceException이 발생한다")
    void run_ociDownloadFails_throwsServiceException() {
        when(ociStorageService.objectName(anyString())).thenAnswer(i -> "data/ai-info/" + i.getArgument(0));
        when(ociStorageService.downloadJson(anyString(), (Class<Object>) any())).thenThrow(new ServiceException(
                back.global.exception.CommonErrorCode.INTERNAL_SERVER_ERROR,
                "[OciStorageServiceImpl#download] OCI 다운로드 실패",
                "OCI 스토리지에서 파일을 다운로드하는데 실패했습니다."
        ));

        assertThatThrownBy(() -> pipelineService.run())
                .isInstanceOf(ServiceException.class);
    }
}