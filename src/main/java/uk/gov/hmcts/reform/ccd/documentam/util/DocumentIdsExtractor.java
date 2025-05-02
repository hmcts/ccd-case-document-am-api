package uk.gov.hmcts.reform.ccd.documentam.util;

import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class DocumentIdsExtractor {
    private DocumentIdsExtractor() {
    }

    public static List<UUID> extractIds(@NotNull final List<DocumentHashToken> documentHashTokens) {
        return documentHashTokens.stream()
            .map(DocumentHashToken::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableList());
    }
}
