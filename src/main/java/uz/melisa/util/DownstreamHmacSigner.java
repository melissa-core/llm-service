package uz.melisa.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.melisa.config.ApplicationProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class DownstreamHmacSigner {

    private final ApplicationProperties applicationProperties;

    private SecretKeySpec key;

    private final ThreadLocal<Mac> macThreadLocal = ThreadLocal.withInitial(() -> {
        try {
            return Mac.getInstance("HmacSHA256");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    });

    @PostConstruct
    void init() {
        String secret = applicationProperties.getDownstream().getHmacSecret();
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String signBase64(String data) {
        try {
            Mac mac = macThreadLocal.get();
            mac.init(key);
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
