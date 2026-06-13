package uz.melisa.util;

import org.springframework.context.i18n.LocaleContextHolder;

public final class LangUtil {

    public static final String UZ = "uz";
    public static final String RU = "ru";
    public static final String EN = "en";

    private LangUtil() {}

    public static String currentLang() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        if (lang == null) return UZ;
        lang = lang.toLowerCase();
        if (lang.startsWith(RU)) return RU;
        if (lang.startsWith(EN)) return EN;
        return UZ;
    }
}