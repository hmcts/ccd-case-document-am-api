package uk.gov.hmcts.reform.ccd.document.am.model.enums;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum SecurityClassification {
    NONE(0),
    PUBLIC(1),
    PRIVATE(2),
    RESTRICTED(3);

    private int hierarchy;

    SecurityClassification(int hierarchy) {
        this.hierarchy = hierarchy;
    }

    public int getHierarchy() {
        return hierarchy;
    }

    public boolean isVisible(int maxHierarchy) {
        return maxHierarchy >= this.getHierarchy();
    }

    /**
     * Converts the hierarchical integer representation of SecurityClassification to the
     * enum representation of SecurityClassification.
     *
     * @param hierarchy Hierarchical integer representation of SecurityClassification
     * @return Enum representation of SecurityClassification
     */
    public static SecurityClassification fromHierarchy(int hierarchy) {
        return Arrays.stream(values())
            .filter(securityClassification -> securityClassification.hierarchy == hierarchy)
            .findFirst()
            .orElseThrow(NoSuchElementException::new);
    }
}
