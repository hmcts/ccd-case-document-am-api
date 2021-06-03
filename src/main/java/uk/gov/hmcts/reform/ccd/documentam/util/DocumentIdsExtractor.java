package uk.gov.hmcts.reform.ccd.documentam.util;

import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DocumentIdsExtractor {
    private DocumentIdsExtractor() {
    }

    public static List<String> extractIds(final List<DocumentHashToken> documentHashTokens) {
        return documentHashTokens.stream()
            .map(DocumentHashToken::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableList());
    }
}
