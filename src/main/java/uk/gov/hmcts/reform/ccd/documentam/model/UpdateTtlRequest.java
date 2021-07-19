package uk.gov.hmcts.reform.ccd.documentam.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTtlRequest {
    @ApiModelProperty
    @NotNull(message = "Provide the TTL")
    private LocalDateTime ttl;
}
