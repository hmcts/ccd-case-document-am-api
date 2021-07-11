package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.util.Date;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.TIMESTAMP_FORMAT;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentCommand {
    @ApiModelProperty
    @JsonProperty("ttl")
    @NotNull(message = "Provide the TTL")
    @JsonFormat(pattern = TIMESTAMP_FORMAT)
    @DateTimeFormat(pattern = TIMESTAMP_FORMAT)
    private Date ttl;
}
