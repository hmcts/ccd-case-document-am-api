package uk.gov.hmcts.reform.ccd.document.am.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.Date;
import java.util.Objects;

/**
 * UpdateDocumentCommand
 */
@Validated
public class UpdateDocumentCommand {
    @JsonProperty("ttl")
    private Date ttl = null;

    public UpdateDocumentCommand ttl(Date ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * Get ttl
     *
     * @return ttl
     **/
    @ApiModelProperty(value = "")

    @Valid
    public Date getTtl() {
        return ttl;
    }

    public void setTtl(Date ttl) {
        this.ttl = ttl;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdateDocumentCommand updateDocumentCommand = (UpdateDocumentCommand) o;
        return Objects.equals(this.ttl, updateDocumentCommand.ttl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ttl);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class UpdateDocumentCommand {\n");

        sb.append("    ttl: ").append(toIndentedString(ttl)).append("\n");
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
