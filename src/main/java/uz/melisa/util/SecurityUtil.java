package uz.melisa.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.melisa.enums.MessageCode;
import uz.melisa.exp.BadRequestException;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        try {
            return Long.parseLong(principal.toString());
        } catch (Exception e) {
            throw new BadRequestException(MessageCode.AUTH_USER_DETAILS_INVALID);
        }
    }
}
