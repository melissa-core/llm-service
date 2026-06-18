package uz.melisa.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

/**
 * Explicit {@link MessageSource} for response-message localization (uz/ru/en, default uz).
 *
 * <p>The {@code AcceptHeaderLocaleResolver} that reads the {@code Accept-Language} header
 * already lives in {@link LocaleConfig}; it is intentionally NOT redeclared here to avoid a
 * duplicate {@code localeResolver} bean.
 */
@Configuration
public class MessageConfig {

    public static final Locale UZ = new Locale("uz");

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(UZ);
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }
}
