package uz.melisa.exp;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.enums.ApiResponseStatus;
import uz.melisa.util.ResponseUtil;

@RestControllerAdvice
@Order(value = Integer.MIN_VALUE)
public class ExceptionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHelper.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleException(Exception e) {
        LOG.error("The error occurred {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("Something went wrong", ApiResponseStatus.INTERNAL_SERVER_ERROR)
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<CommonResponse<String>> handleBadCredentialsException(BadCredentialsException e) {
        LOG.error("The credential is wrong {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("Username or password wrong", ApiResponseStatus.UNAUTHORIZED)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        LOG.error("The validation error {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("", ApiResponseStatus.INVALID_PARAMETER)
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<CommonResponse<String>> handleException(EntityNotFoundException e) {
        LOG.error("The entity not found {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("Entity not found", ApiResponseStatus.NOT_FOUND)
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleException(UsernameNotFoundException e) {
        LOG.error("The pinfl not found {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("Username not found", ApiResponseStatus.NOT_FOUND)
        );
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleException(ItemNotFoundException e) {
        LOG.error("The item not found {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(e.getMessage(), ApiResponseStatus.NOT_FOUND)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        LOG.error("The type mismatch {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("Invalid parameter", ApiResponseStatus.BAD_REQUEST)
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleNoResourceFoundException(NoResourceFoundException e) {
        LOG.error("The resource not found {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("No static resource found", ApiResponseStatus.NO_STATIC_RESOURCES)
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleNoResourceFoundException(AuthenticationException e) {
        LOG.error("The unauthenticated user {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("Unauthenticated user", ApiResponseStatus.UNAUTHORIZED)
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleNoResourceFoundException(BadRequestException e) {
        LOG.error("BadRequestException : {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure("Bad request", ApiResponseStatus.BAD_REQUEST)
        );
    }
}
