package back.domain.info.controller;

import back.domain.info.dto.response.PageUpdateRequestResponse;
import back.domain.info.dto.response.UpdateRequestResponse;
import back.domain.info.service.AiInfoService;
import back.domain.info.service.ModelBenchmarkService;
import back.domain.info.service.UpdateRequestService;
import back.global.response.RsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiInfoControllerTest {

    private AiInfoService aiInfoService;
    private ModelBenchmarkService benchmarkService;
    private UpdateRequestService updateRequestService;
    private AiInfoController controller;

    @BeforeEach
    void setUp() {
        aiInfoService = mock(AiInfoService.class);
        benchmarkService = mock(ModelBenchmarkService.class);
        updateRequestService = mock(UpdateRequestService.class);
        controller = new AiInfoController(aiInfoService, benchmarkService, updateRequestService);
    }

    @Test
    void getModel_runsAiInfoService() throws Exception {
        ResponseEntity<RsData<Void>> response = controller.getModel();

        verify(aiInfoService).run();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isNotBlank();
    }

    @Test
    void getBenchmark_runsBenchmarkService() throws Exception {
        ResponseEntity<RsData<Void>> response = controller.getBenchmark();

        verify(benchmarkService).run();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void run_executesUpdateImport() throws Exception {
        ResponseEntity<RsData<Void>> response = controller.run();

        verify(updateRequestService).run();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updateStatus_delegatesToService() {
        ResponseEntity<RsData<Void>> response = controller.updateStatus(7L, "approved");

        verify(updateRequestService).updateStatus(7L, "approved");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getUpdateApproved_returnsServiceResult() {
        PageUpdateRequestResponse serviceResponse = new PageUpdateRequestResponse(
                new PageImpl<>(List.<UpdateRequestResponse>of(), PageRequest.of(0, 10), 0)
        );
        when(updateRequestService.getUpdatesApproved(PageRequest.of(0, 10))).thenReturn(serviceResponse);

        PageUpdateRequestResponse response = controller.getUpdateApproved(PageRequest.of(0, 10));

        assertThat(response).isSameAs(serviceResponse);
    }

    @Test
    void getUpdate_returnsServiceResult() {
        PageUpdateRequestResponse serviceResponse = new PageUpdateRequestResponse(
                new PageImpl<>(List.<UpdateRequestResponse>of(), PageRequest.of(1, 5), 0)
        );
        when(updateRequestService.getUpdates(PageRequest.of(1, 5))).thenReturn(serviceResponse);

        PageUpdateRequestResponse response = controller.getUpdate(PageRequest.of(1, 5));

        assertThat(response).isSameAs(serviceResponse);
    }
}
