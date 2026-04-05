package back.domain.aimodel.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AiModelProperties.class,
        OciProperties.class,
        GeminiProperties.class
})
public class AiModelConfig {}
