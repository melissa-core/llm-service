package uz.melisa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CatalogRestClientConfig {

    @Bean("catalogService")
    public RestClient llmRestClient(RestClient.Builder builder, ApplicationProperties props) {
        return builder.baseUrl(props.getDownstream().getCatalogServiceUrl()).build();
    }
}
