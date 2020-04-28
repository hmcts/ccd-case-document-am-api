package uk.gov.hmcts.reform.ccd.document.am.configuration;

import org.springframework.http.MediaType;

public class V1MediaType extends MediaType {

    public static final long serialVersionUID = 112234;

    public static final String V1_HAL_DOCUMENT_COLLECTION_MEDIA_TYPE_VALUE =
        "application/vnd.uk.gov.hmcts.ccd-case-document-am-api.document-collection.v1+hal+json;charset=UTF-8";

    public static final MediaType V1_HAL_DOCUMENT_COLLECTION_MEDIA_TYPE = valueOf(V1_HAL_DOCUMENT_COLLECTION_MEDIA_TYPE_VALUE);

    public static final String V1_HAL_DOCUMENT_MEDIA_TYPE_VALUE = "application/vnd.uk.gov.hmcts.ccd-case-document-am-api.document.v1+hal+json;charset=UTF-8";

    public static final MediaType V1_HAL_DOCUMENT_MEDIA_TYPE = valueOf(V1_HAL_DOCUMENT_MEDIA_TYPE_VALUE);

    public V1MediaType(String type, String subtype) {
        super(type, subtype);
    }

}
