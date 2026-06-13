package uz.melisa.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.melisa.enums.ApiResponseStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonResponse<T> {

    private T data;
    private String errorMessage;

    @JsonIgnore
    private ApiResponseStatus status;

    private CommonResponse(T data) {
        this.data = data;
        this.status = ApiResponseStatus.OK;
    }

    private CommonResponse(String message, ApiResponseStatus status) {
        this.data = null;
        this.errorMessage = message;
        this.status = status;
    }

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(data);
    }

    public static <T> CommonResponse<T> failure(String message, ApiResponseStatus status) {
        return new CommonResponse<>(message, status);
    }
}