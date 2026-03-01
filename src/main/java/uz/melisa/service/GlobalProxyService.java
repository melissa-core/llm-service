package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.melisa.config.ApplicationProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GlobalProxyService {

    private final ApplicationProperties props;

    public Map<String, String> buildInternalHeaders(Long userId) {
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

        return Map.of(
                "X-User-Id", String.valueOf(userId),
                "X-User-Roles", roles,
                "X-Auth-Signature", signature
        );
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
}
