package back.global.config.properties;

import back.domain.aimodel.config.AiModelProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        OciProperties.class,
        GeminiProperties.class,
        AiModelProperties.class,
        AiInfoProperties.class,
        AiTrackerProperties.class,
        GithubProperties.class
})
public class PropertiesConfig {}