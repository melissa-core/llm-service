package uz.melisa.exp;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
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
import uz.melisa.enums.MessageCode;
import uz.melisa.service.LocalizationService;
import uz.melisa.util.ResponseUtil;

import java.util.Locale;

@RestControllerAdvice
@Order(value = Integer.MIN_VALUE)
@RequiredArgsConstructor
public class ExceptionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHelper.class);

    private final LocalizationService localizationService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleBusinessException(BusinessException e, Locale locale) {
        LOG.warn("Business exception: code={}, status={}, detail={}", e.getMessageCode(), e.getStatus(), e.getMessage());
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(resolveBusinessMessage(e, locale), e.getStatus())
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleBadCredentialsException(BadCredentialsException e, Locale locale) {
        LOG.warn("The credential is wrong: {}", e.getMessage());
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(localizationService.getMessage(MessageCode.AUTH_INVALID_CREDENTIALS, locale), ApiResponseStatus.UNAUTHORIZED)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, Locale locale) {
        LOG.warn("The validation error: {}", e.getMessage());
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(localizationService.getMessage(MessageCode.COMMON_INVALID_INPUT, locale), ApiResponseStatus.INVALID_PARAMETER)
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleEntityNotFoundException(EntityNotFoundException e, Locale locale) {
        LOG.warn("The entity not found: {}", e.getMessage());
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(localizationService.getMessage(MessageCode.COMMON_DATA_NOT_FOUND, locale), ApiResponseStatus.NOT_FOUND)
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleUsernameNotFoundException(UsernameNotFoundException e, Locale locale) {
        LOG.warn("The username not found: {}", e.getMessage());
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(localizationService.getMessage(MessageCode.AUTH_USER_NOT_FOUND, locale), ApiResponseStatus.NOT_FOUND)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, Locale locale) {
        LOG.warn("The type mismatch: {}", e.getMessage());
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(localizationService.getMessage(MessageCode.COMMON_INVALID_INPUT, locale), ApiResponseStatus.BAD_REQUEST)
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleNoResourceFoundException(NoResourceFoundException e, Locale locale) {
        LOG.warn("The resource not found: {}", e.getMessage());
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(localizationService.getMessage(MessageCode.COMMON_PAGE_NOT_FOUND, locale), ApiResponseStatus.NO_STATIC_RESOURCES)
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleAuthenticationException(AuthenticationException e, Locale locale) {
        LOG.warn("The unauthenticated user: {}", e.getMessage());
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(localizationService.getMessage(MessageCode.AUTH_SESSION_INVALID, locale), ApiResponseStatus.UNAUTHORIZED)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> handleException(Exception e, HttpServletResponse response, Locale locale) {
        // The response may already be committed (e.g. an SSE stream that emits its own
        // in-stream 'error' event). Writing a CommonResponse now would corrupt the stream
        // and trigger an /error re-dispatch, so suppress the body and only log.
        if (responseAlreadyGone(response)) {
            LOG.warn("Error after response committed/streaming, suppressing body: {}", e.getMessage());
            return ResponseEntity.noContent().build();
        }
        LOG.error("The error occurred: {}", e.getMessage(), e);
        return ResponseUtil.buildResponseDTO(
                CommonResponse.failure(localizationService.getMessage(MessageCode.COMMON_SOMETHING_WENT_WRONG, locale), ApiResponseStatus.INTERNAL_SERVER_ERROR)
        );
    }

    private String resolveBusinessMessage(BusinessException e, Locale locale) {
        if (e.getMessageCode() != null) {
            return localizationService.getMessage(e.getMessageCode(), locale);
        }
        // Raw-String business exceptions never echo their (English/internal) text to the
        // client; they collapse to the generic localized message. Only the status is honored.
        return localizationService.getMessage(MessageCode.COMMON_SOMETHING_WENT_WRONG, locale);
    }

    private boolean responseAlreadyGone(HttpServletResponse response) {
        if (response == null) {
            return false;
        }
        if (response.isCommitted()) {
            return true;
        }
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
