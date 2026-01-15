package uz.melisa.util;

import org.springframework.http.ResponseEntity;
import uz.melisa.dto.common.CommonResponse;

public final class ResponseUtil {

    private ResponseUtil() {
    }

    public static <T> ResponseEntity<CommonResponse<T>> buildResponseDTO(final CommonResponse<T> commonResponse) {
        return ResponseEntity.status(commonResponse.getStatus().getHttpStatus()).body(commonResponse);
    }
}
