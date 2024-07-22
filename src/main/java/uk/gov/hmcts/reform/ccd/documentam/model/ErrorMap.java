package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Validated
public class ErrorMap {
    @JsonProperty("code")
    private String code = null;

    @JsonProperty("message")
    private String message = null;

    @Schema(requiredMode = RequiredMode.REQUIRED, description = "The error code")
    @NotNull
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Schema(requiredMode = RequiredMode.REQUIRED, description = "The error message")
    @NotNull

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ErrorMap errorMap = (ErrorMap) o;
        return Objects.equals(this.code, errorMap.code)
               && Objects.equals(this.message, errorMap.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ErrorMap {\n");

        sb.append("    code: ").append(toIndentedString(code)).append("\n");
        sb.append("    message: ").append(toIndentedString(message)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
