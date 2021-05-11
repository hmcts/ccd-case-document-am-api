package uk.gov.hmcts.reform.ccd.documentam.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.ccd.documentam.TestConstants.RANDOM_UUID;

class DocumentIdsExtractorTest {

    @Test
    @DisplayName("Test should return empty list when documentHashTokens is an empty list")
    void testWhenDocumentHashTokensAreEmpty() {

        final List<DocumentHashToken> documentHashTokens = List.of();

        final List<String> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

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

        final List<String> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .isNotNull()
            .isEmpty();

    }

    @Test
    @DisplayName("Test should return a single Id list when documentHashTokens has one element with Id")
    void testWhenDocumentHashTokensHasOneItem() {

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder()
                .id(RANDOM_UUID.toString())
                .build()
        );

        final List<String> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .singleElement()
            .isEqualTo(RANDOM_UUID.toString());

    }

    @Test
    @DisplayName("Test should return a multi Id list when documentHashTokens has multiple elements with Ids")
    void testWhenDocumentHashTokensHasMultipleItems() {

        final UUID id1 = UUID.randomUUID();
        final UUID id2 = UUID.randomUUID();

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder()
                .id(id1.toString())
                .build(),
            DocumentHashToken.builder()
                .id(id2.toString())
                .build()
        );

        final List<String> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .hasSameElementsAs(List.of(id1.toString(), id2.toString()));

    }

    @Test
    @DisplayName("Test should filter out null Ids")
    void testFilterOutNullIds() {

        final List<DocumentHashToken> documentHashTokens = List.of(
            DocumentHashToken.builder()
                .id(RANDOM_UUID.toString())
                .build(),
            DocumentHashToken.builder()
                .build()
        );

        final List<String> actualIds = DocumentIdsExtractor.extractIds(documentHashTokens);

        assertThat(actualIds)
            .singleElement()
            .isEqualTo(RANDOM_UUID.toString());

    }

}
