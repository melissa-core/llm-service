package uz.melisa.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        Timeout connectTimeout = Timeout.of(Duration.ofMinutes(3));
        Timeout responseTimeout = Timeout.of(Duration.ofMinutes(3));
        Timeout connectionRequestTimeout = Timeout.of(Duration.ofMinutes(3));

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(connectTimeout)
                .build();

        PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(50)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(responseTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
