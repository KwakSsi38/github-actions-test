package back.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final String STORAGE_TYPE_LOCAL = "local";

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.embedding.base-url:https://embedding-server-domain}")
    private String embeddingBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!STORAGE_TYPE_LOCAL.equalsIgnoreCase(storageType)) {
            return;
        }

        // 업로드된 파일을 정적 리소스로 제공
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadDir + "/");
    }

    @Bean
    public WebClient embeddingWebClient() {
        // 기본 256KB → 임베딩 배치 응답(32개 × 1024차원 JSON) 초과 방지를 위해 10MB로 확장
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(embeddingBaseUrl)
                .exchangeStrategies(exchangeStrategies)
                .build();
    }
}
