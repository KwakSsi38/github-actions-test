package back.global.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;

@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "oci")
public class OciObjectStorageConfig {

    @Bean
    AbstractAuthenticationDetailsProvider ociAuthenticationDetailsProvider(
            @Value("${oci.auth-type:config-file}") String authType,
            @Value("${oci.config-file-path:}") String configFilePath,
            @Value("${oci.profile:DEFAULT}") String profile
    ) throws IOException {
        if ("instance-principal".equalsIgnoreCase(authType)) {
            return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        }

        ConfigFileReader.ConfigFile configFile = configFilePath == null || configFilePath.isBlank()
                ? ConfigFileReader.parseDefault(profile)
                : ConfigFileReader.parse(configFilePath, profile);

        return new ConfigFileAuthenticationDetailsProvider(configFile);
    }

    @Bean(destroyMethod = "close")
    ObjectStorage objectStorage(
            AbstractAuthenticationDetailsProvider authenticationDetailsProvider,
            @Value("${oci.region:}") String region
    ) {
        ObjectStorageClient client = ObjectStorageClient.builder().build(authenticationDetailsProvider);
        if (region != null && !region.isBlank()) {
            client.setRegion(region);
        }
        return client;
    }
}
