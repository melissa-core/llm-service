package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uz.melisa.config.ApplicationProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CatalogProxyService {

    private final RestClient catalogRestClient;
    private final ApplicationProperties props;

    public CatalogProxyService(
            @Qualifier("catalogService") RestClient catalogRestClient,
            ApplicationProperties props
    ) {
        this.catalogRestClient = catalogRestClient;
        this.props = props;
    }

    public String activateChat(Long userId, String key) {
        AuthHeaders h = buildInternalHeaders(userId);

        return catalogRestClient.post()
                .uri("/api/v1/chat/activate-chat/{key}", key)
                .header("X-User-Id", String.valueOf(h.userId()))
                .header("X-User-Roles", h.roles())
                .header("X-Auth-Signature", h.signature())
                .retrieve()
                .body(String.class);
    }

    private AuthHeaders buildInternalHeaders(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String roles = "ROLE_GUEST";

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            if (roles.isBlank()) roles = "ROLE_USER";
        }

        String payload = userId + "|" + roles;
        String signature = hmacSha256Base64(payload, props.getDownstream().getHmacSecret());

        return new AuthHeaders(userId, roles, signature);
    }

    private static String hmacSha256Base64(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record AuthHeaders(Long userId, String roles, String signature) {
    }
}
