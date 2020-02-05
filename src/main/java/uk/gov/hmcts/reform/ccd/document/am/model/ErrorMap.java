package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * ErrorMap
 */
@Validated
public class ErrorMap {
    @JsonProperty("code")
    private String code = null;

    @JsonProperty("message")
    private String message = null;

    public ErrorMap code(String code) {
        this.code = code;
        return this;
    }

    /**
     * The error code
     *
     * @return code
     **/
    @ApiModelProperty(required = true, value = "The error code")
    @NotNull

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ErrorMap message(String message) {
        this.message = message;
        return this;
    }

    /**
     * The error message
     *
     * @return message
     **/
    @ApiModelProperty(required = true, value = "The error message")
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
        return Objects.equals(this.code, errorMap.code) &&
            Objects.equals(this.message, errorMap.message);
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

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
