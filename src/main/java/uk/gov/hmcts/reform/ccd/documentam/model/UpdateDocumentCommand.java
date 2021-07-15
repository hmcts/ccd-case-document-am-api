package uk.gov.hmcts.reform.ccd.documentam.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.TIMESTAMP_FORMAT;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentCommand {
    @ApiModelProperty
    @NotNull(message = "Provide the TTL")
    @DateTimeFormat(pattern = TIMESTAMP_FORMAT)
    private LocalDateTime ttl;
}
