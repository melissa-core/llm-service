package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uz.melisa.exp.RemoteServiceException;
import uz.melisa.exp.RemoteServiceTimeoutException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestSenderService {

    private final RestClient restClient;

    public <T, R> R sendAndReceive(String url, HttpMethod method, T requestBody, Class<R> responseType) {
        return sendAndReceive(url, method, requestBody, responseType, null);
    }

    public <T, R> R sendAndReceive(String url,
                                   HttpMethod method,
                                   T requestBody,
                                   ParameterizedTypeReference<R> responseType,
                                   Map<String, String> headers) {
        log.info("Sending {} request to {}", method, url);
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(url);
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(spec::header);
            }
            if (requestBody != null) {
                spec.body(requestBody);
            }
            R response = spec.retrieve().body(responseType);
            log.info("Received response from {} {}: {}", method, url, response);
            return response;

        } catch (HttpStatusCodeException e) {
            log.error("HTTP error while calling {} {} -> Status: {}, Body: {}",
                    method, url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RemoteServiceException(
                    "Remote API returned error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                log.error("Timeout while calling {} {}: {}", method, url, cause.getMessage());
                throw new RemoteServiceTimeoutException("Connection timed out while calling remote API", e);
            } else if (cause instanceof UnknownHostException) {
                log.error("Unknown host while calling {} {}: {}", method, url, cause.getMessage());
                throw new RemoteServiceException("Unknown host or DNS resolution failed", e);
            } else {
                log.error("Connection error while calling {} {}: {}", method, url, e.getMessage(), e);
                throw new RemoteServiceException("Connection error while calling remote API", e);
            }
        } catch (RestClientException e) {
            log.error("RestClient error during {} {}: {}", method, url, e.getMessage(), e);
            throw new RemoteServiceException("Failed to call remote API: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error during {} {}: {}", method, url, e.getMessage(), e);
            throw new RemoteServiceException("Unexpected error while calling remote API", e);
        }
    }

    public <T, R> R sendAndReceive(String url, HttpMethod method, T requestBody,
                                   Class<R> responseType, Map<String, String> headers) {
        log.info("Sending {} request to {}", method, url);

        try {
            RestClient.RequestBodySpec requestBodySpec = restClient.method(method).uri(url);

            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBodySpec.header(header.getKey(), header.getValue());
                }
            }

            if (requestBody != null) {
                requestBodySpec.body(requestBody);
            }

            R response = requestBodySpec
                    .retrieve()
                    .body(responseType);

            log.info("Received response from {} {}: {}", method, url, response);
            return response;
        } catch (HttpStatusCodeException e) {
            log.error("HTTP error while calling {} {} -> Status: {}, Body: {}",
                    method, url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RemoteServiceException("Remote API returned error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                log.error("Timeout while calling {} {}: {}", method, url, cause.getMessage());
                throw new RemoteServiceTimeoutException("Connection timed out while calling remote API", e);
            } else if (cause instanceof UnknownHostException) {
                log.error("Unknown host while calling {} {}: {}", method, url, cause.getMessage());
                throw new RemoteServiceException("Unknown host or DNS resolution failed", e);
            } else {
                log.error("Connection error while calling {} {}: {}", method, url, e.getMessage());
                throw new RemoteServiceException("Connection error while calling remote API", e);
            }
        } catch (RestClientException e) {
            log.error("RestClient error during {} {}: {}", method, url, e.getMessage(), e);
            throw new RemoteServiceException("Failed to call remote API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during {} {}: {}", method, url, e.getMessage(), e);
            throw new RemoteServiceException("Unexpected error while calling remote API", e);
        }
    }
}
