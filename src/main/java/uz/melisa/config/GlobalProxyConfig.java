package uz.melisa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.melisa.util.DownstreamHmacSigner;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class GlobalProxyConfig {

    private final DownstreamHmacSigner downstreamHmacSigner;

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
        String signature = downstreamHmacSigner.signBase64(payload);

        return Map.of(
                "X-User-Id", String.valueOf(userId),
                "X-User-Roles", roles,
                "X-Auth-Signature", signature
        );
    }
}
