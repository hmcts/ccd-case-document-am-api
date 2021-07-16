package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.TIMESTAMP_FORMAT;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentCommand {
    @ApiModelProperty
    @NotNull(message = "Provide the TTL")
    @JsonFormat(pattern = TIMESTAMP_FORMAT, lenient = OptBoolean.FALSE)
    private LocalDateTime ttl;
}
