package uk.gov.hmcts.reform.ccd.documentam.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DocumentIdsExtractorTest implements TestFixture {

    @Test
    @DisplayName("Test should return empty list when documentHashTokens is an empty list")
    void testWhenDocumentHashTokensAreEmpty() {

        final List<DocumentHashToken> documentHashTokens = List.of();

        final List<UUID> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .isNotNull()
            .isEmpty();

    }

    @Test
    @DisplayName("Test should return empty list when documentHashTokens has elements but Id is not set")
    void testWhenDocumentHashTokensHasItemsButIdIsNotSet() {

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder().build()
        );

        final List<UUID> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .isNotNull()
            .isEmpty();

    }

    @Test
    @DisplayName("Test should return a single Id list when documentHashTokens has one element with Id")
    void testWhenDocumentHashTokensHasOneItem() {

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder()
                .id(DOCUMENT_ID)
                .build()
        );

        final List<UUID> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .singleElement()
            .isEqualTo(DOCUMENT_ID);

    }

    @Test
    @DisplayName("Test should return a multi Id list when documentHashTokens has multiple elements with Ids")
    void testWhenDocumentHashTokensHasMultipleItems() {

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder()
                .id(DOCUMENT_ID_1)
                .build(),
            DocumentHashToken.builder()
                .id(DOCUMENT_ID_2)
                .build()
        );

        final List<UUID> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .hasSameElementsAs(List.of(DOCUMENT_ID_1, DOCUMENT_ID_2));

    }

    @Test
    @DisplayName("Test should filter out null Ids")
    void testFilterOutNullIds() {

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder()
                .id(DOCUMENT_ID)
                .build(),
            DocumentHashToken.builder()
                .build()
        );

        final List<UUID> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .singleElement()
            .isEqualTo(DOCUMENT_ID);

    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testShouldRaiseNullPointerExceptionWhenInputIsNull() {
        assertThatNullPointerException().isThrownBy(() -> DocumentIdsExtractor.extractIds(null));
    }

}
