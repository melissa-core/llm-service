package uz.melisa.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.melisa.enums.ApiResponseStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Standard API response envelope. On success 'data' holds the payload; "
        + "on failure 'errorMessage' holds the error text. Null fields are omitted from the JSON.")
public class CommonResponse<T> {

    @Schema(description = "Response payload, present on success")
    private T data;

    @Schema(description = "Human-readable error text, present on failure", example = "Entity not found")
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