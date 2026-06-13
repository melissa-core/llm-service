package uz.melisa.util;

public final class StringUtil {

    private StringUtil() {}

    public static Long parseChatIdLong(String chatId) {
        String s = trimToEmpty(chatId);
        if (s.isEmpty()) return null;
        String digits = s.replaceAll("\\D+", "");
        if (digits.isEmpty()) return null;
        try {
            return Long.parseLong(digits);
        } catch (Exception e) {
            return null;
        }
    }

    public static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
