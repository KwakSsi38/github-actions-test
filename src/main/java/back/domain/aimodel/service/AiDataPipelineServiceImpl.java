package back.domain.aimodel.service;

import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse;
import back.domain.aimodel.dto.integrated.BenchmarkRecord;
import back.domain.aimodel.dto.integrated.IntegratedVendor;
import back.domain.aimodel.dto.openrouter.OrModelsResponse;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.infra.oci.OciStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDataPipelineServiceImpl implements AiDataPipelineService {

    private static final String RAW_OR_FILE     = "ai-info/models_info_raw.json";
    private static final String RAW_AA_FILE     = "ai-info/models_benchmark_raw.json";
    private static final String INTEGRATED_FILE = "ai-info/integrated_major_models.json";
    private static final String BENCHMARKS_FILE = "ai-info/model_benchmarks_records.json";

    private final OciStorageService  ociStorageService;
    private final ModelMergeService  modelMergeService;
    private final BenchmarkService   benchmarkService;
    private final DescriptionService descriptionService;

    @Override
    public void run() {
        log.info("=== AI 모델 데이터 파이프라인 시작 ===");
        long start = System.currentTimeMillis();

        try {
            // ── 1. raw 데이터 다운로드 ────────────────────────────────────────
            log.info("[1/5] OCI raw 데이터 다운로드");
            OrModelsResponse orResponse = ociStorageService.downloadJson(
                    ociStorageService.objectName(RAW_OR_FILE), OrModelsResponse.class);
            AaModelsResponse aaResponse = ociStorageService.downloadJson(
                    ociStorageService.objectName(RAW_AA_FILE), AaModelsResponse.class);
            log.info("  OR 모델: {}개 / AA 모델: {}개",
                    orResponse.data().size(), aaResponse.data().size());

            // ── 2. OR 기반 패밀리 그룹핑 ─────────────────────────────────────
            log.info("[2/5] 패밀리 그룹핑 (빅3 벤더 필터 + OR 기반)");
            List<IntegratedVendor> integrated = modelMergeService.merge(orResponse);
            log.info("  통합 벤더: {}개", integrated.size());

            // ── 3. description 생성 → integrated에 직접 반영 ─────────────────
            // 실패해도 기존 빈값 유지하고 파이프라인 계속 진행
            log.info("[3/5] common_description 생성 (Gemini)");
            try {
                integrated = descriptionService.generateAndApply(integrated);
            } catch (Exception e) {
                log.warn("  description 생성 단계 실패 —  빈값으로 업로드 진행", e);
            }

            // ── 4. integrated 업로드 ─────────────────────────────────────────
            log.info("[4/5] integrated_major_models.json 업로드");
            ociStorageService.uploadJson(
                    ociStorageService.objectName(INTEGRATED_FILE), integrated);

            // ── 5. AA 기반 벤치마크 레코드 추출 + 업로드 ─────────────────────
            log.info("[5/5] 벤치마크 레코드 추출 및 업로드");
            List<BenchmarkRecord> benchmarks = benchmarkService.extract(aaResponse);
            log.info("  벤치마크 레코드: {}개", benchmarks.size());
            ociStorageService.uploadJson(
                    ociStorageService.objectName(BENCHMARKS_FILE), benchmarks);

            long elapsed = System.currentTimeMillis() - start;
            log.info("=== 파이프라인 완료 ({} ms) ===", elapsed);

        } catch (Exception e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[AiDataPipelineServiceImpl#run] 파이프라인 실패: " + e.getMessage(),
                    "AI 데이터 파이프라인 실행 중 오류가 발생했습니다."
            );
        }
    }
}