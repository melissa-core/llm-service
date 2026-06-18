package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import uz.melisa.enums.MessageCode;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalizationService {

    private static final Locale UZBEK = new Locale("uz");
    private static final Locale RUSSIAN = new Locale("ru");

    private final MessageSource messageSource;

    /**
     * Resolves a localized message using the locale of the current request
     * (Accept-Language). Use this on the servlet request thread (e.g. success
     * messages built inside services); for the error path the resolved
     * {@link Locale} is passed explicitly from the exception handler.
     */
    public String getMessage(MessageCode code, Object... args) {
        return getMessage(code, resolveRequestLocale(), args);
    }

    public String getMessage(MessageCode code, Locale locale, Object... args) {
        Locale normalizedLocale = normalize(locale);

        String message = messageSource.getMessage(
                code.getKey(),
                args,
                code.getKey(),
                normalizedLocale
        );

        if (Objects.equals(message, code.getKey())) {
            log.warn("Localization key not resolved: key={}, locale={}", code.getKey(), normalizedLocale);
        }

        return message;
    }

    private Locale resolveRequestLocale() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return RequestContextUtils.getLocale(servletRequestAttributes.getRequest());
        }
        return LocaleContextHolder.getLocale();
    }

    private Locale normalize(Locale locale) {
        if (locale == null) {
            return UZBEK;
        }
        String language = locale.getLanguage();
        if ("ru".equals(language)) {
            return RUSSIAN;
        }
        if ("en".equals(language)) {
            return Locale.ENGLISH;
        }
        return UZBEK;
    }
}
