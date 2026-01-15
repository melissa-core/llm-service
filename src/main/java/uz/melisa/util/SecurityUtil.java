package uz.melisa.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.melisa.exp.BadRequestException;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        try {
            return (Long) principal;
        } catch (Exception e) {
            throw new BadRequestException("User details could not parsed");
        }
    }
}
