package back.domain.aimodel.dto;

import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse;
import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse.AaModel;
import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse.AaModel.ModelCreator;
import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse.AaModel.Pricing;
import back.domain.aimodel.dto.artificialanalysis.AaModelsResponse.AaModel.Evaluations;
import back.domain.aimodel.dto.openrouter.OrModelsResponse;
import back.domain.aimodel.dto.openrouter.OrModelsResponse.OrModel;
import back.domain.aimodel.dto.openrouter.OrModelsResponse.OrModel.Architecture;
import back.domain.aimodel.dto.openrouter.OrModelsResponse.OrModel.TopProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DTO record 클래스들의 JaCoCo 커버리지를 충족하기 위한 테스트.
 *
 * 커버리지 대상:
 * - AaModelsResponse.AaModel.ModelCreator
 * - OrModelsResponse.OrModel.Architecture
 * - OrModelsResponse.OrModel.TopProvider
 * - OrModelsResponse.OrModel.Pricing
 */
class DtoRecordCoverageTest {

    // ── AaModelsResponse ──────────────────────────────────────────────────────

    @Test
    @DisplayName("AaModelsResponse - null data 입력 시 빈 리스트로 초기화된다")
    void aaModelsResponse_nullData_returnsEmptyList() {
        AaModelsResponse response = new AaModelsResponse(null);
        assertThat(response.data()).isEmpty();
    }

    @Test
    @DisplayName("AaModelsResponse - 정상 data 입력 시 방어적 복사된 리스트를 반환한다")
    void aaModelsResponse_withData_returnsCopiedList() {
        ModelCreator creator = new ModelCreator("Anthropic", "anthropic");
        Pricing pricing = new Pricing(3.0, 15.0, 7.5);
        Evaluations evals = new Evaluations(85.0, 90.0, 88.0);
        AaModel model = new AaModel(
                "claude-3-opus", "Claude 3 Opus",
                "2024-03-01", 200000,
                creator, pricing, evals,
                95.0, 0.3
        );

        AaModelsResponse response = new AaModelsResponse(List.of(model));
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).slug()).isEqualTo("claude-3-opus");
    }

    @Test
    @DisplayName("ModelCreator - name과 slug 필드가 올바르게 저장된다")
    void modelCreator_fieldsStoredCorrectly() {
        ModelCreator creator = new ModelCreator("OpenAI", "openai");

        assertThat(creator.name()).isEqualTo("OpenAI");
        assertThat(creator.slug()).isEqualTo("openai");
    }

    @Test
    @DisplayName("ModelCreator - null 필드도 허용된다")
    void modelCreator_nullFields_allowed() {
        ModelCreator creator = new ModelCreator(null, null);

        assertThat(creator.name()).isNull();
        assertThat(creator.slug()).isNull();
    }

    @Test
    @DisplayName("AaModel.Pricing - 세 가지 가격 필드가 올바르게 저장된다")
    void aaModelPricing_fieldsStoredCorrectly() {
        Pricing pricing = new Pricing(1.0, 5.0, 2.5);

        assertThat(pricing.price1mInputTokens()).isEqualTo(1.0);
        assertThat(pricing.price1mOutputTokens()).isEqualTo(5.0);
        assertThat(pricing.price1mBlended3to1()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("AaModel.Evaluations - 세 가지 인덱스 필드가 올바르게 저장된다")
    void aaModelEvaluations_fieldsStoredCorrectly() {
        Evaluations evals = new Evaluations(80.0, 75.0, 70.0);

        assertThat(evals.intelligenceIndex()).isEqualTo(80.0);
        assertThat(evals.codingIndex()).isEqualTo(75.0);
        assertThat(evals.mathIndex()).isEqualTo(70.0);
    }

    // ── OrModelsResponse ──────────────────────────────────────────────────────

    @Test
    @DisplayName("OrModelsResponse - null data 입력 시 빈 리스트로 초기화된다")
    void orModelsResponse_nullData_returnsEmptyList() {
        OrModelsResponse response = new OrModelsResponse(null);
        assertThat(response.data()).isEmpty();
    }

    @Test
    @DisplayName("Architecture - null 입력 시 빈 리스트로 초기화된다")
    void architecture_nullInputs_returnsEmptyLists() {
        Architecture arch = new Architecture(null, null);

        assertThat(arch.inputModalities()).isEmpty();
        assertThat(arch.outputModalities()).isEmpty();
    }

    @Test
    @DisplayName("Architecture - 정상 입력 시 방어적 복사된 리스트를 반환한다")
    void architecture_withData_returnsCopiedLists() {
        Architecture arch = new Architecture(
                List.of("text", "image"),
                List.of("text")
        );

        assertThat(arch.inputModalities()).containsExactly("text", "image");
        assertThat(arch.outputModalities()).containsExactly("text");
    }

    @Test
    @DisplayName("OrModel.Pricing - prompt와 completion 필드가 올바르게 저장된다")
    void orModelPricing_fieldsStoredCorrectly() {
        OrModel.Pricing pricing = new OrModel.Pricing("0.000001", "0.000002");

        assertThat(pricing.prompt()).isEqualTo("0.000001");
        assertThat(pricing.completion()).isEqualTo("0.000002");
    }

    @Test
    @DisplayName("OrModel.Pricing - null 필드도 허용된다")
    void orModelPricing_nullFields_allowed() {
        OrModel.Pricing pricing = new OrModel.Pricing(null, null);

        assertThat(pricing.prompt()).isNull();
        assertThat(pricing.completion()).isNull();
    }

    @Test
    @DisplayName("TopProvider - maxCompletionTokens 필드가 올바르게 저장된다")
    void topProvider_fieldStoredCorrectly() {
        TopProvider topProvider = new TopProvider(4096);

        assertThat(topProvider.maxCompletionTokens()).isEqualTo(4096);
    }

    @Test
    @DisplayName("TopProvider - null maxCompletionTokens도 허용된다")
    void topProvider_nullField_allowed() {
        TopProvider topProvider = new TopProvider(null);

        assertThat(topProvider.maxCompletionTokens()).isNull();
    }

    @Test
    @DisplayName("OrModel - 모든 필드를 포함한 전체 구성이 올바르게 동작한다")
    void orModel_fullConstruction_correct() {
        Architecture arch = new Architecture(List.of("text"), List.of("text"));
        OrModel.Pricing pricing = new OrModel.Pricing("0.000001", "0.000002");
        TopProvider topProvider = new TopProvider(8192);

        OrModel model = new OrModel(
                "openai/gpt-4o", "GPT-4o", "Flagship model",
                128000, arch, pricing, topProvider
        );
        OrModelsResponse response = new OrModelsResponse(List.of(model));

        OrModel retrieved = response.data().get(0);
        assertThat(retrieved.id()).isEqualTo("openai/gpt-4o");
        assertThat(retrieved.architecture().inputModalities()).containsExactly("text");
        assertThat(retrieved.pricing().prompt()).isEqualTo("0.000001");
        assertThat(retrieved.topProvider().maxCompletionTokens()).isEqualTo(8192);
    }
}
