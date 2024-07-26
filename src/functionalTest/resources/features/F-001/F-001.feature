@F-001
Feature: F-001: Get Document Metadata by Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-001 @Retryable(statusCodes={400,409,502},match={"\"BEFTA_CASETYPE_2_1"})
  Scenario: must successfully get document metadata by document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And   another successful call [to create a case of this case type] as in [S-001_Case_Create]
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API] with a delay of [30] seconds [before] the call,
    Then  a positive response is received,
    And   the response [contains the metadata for the document uploaded above],
    And   the response has all other details as expected.

  @S-002
  Scenario: must get an error response for a non-existing document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [for a non-existing document ID],
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-003
  Scenario: must get an error response for a malformed document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [for a malformed document ID],
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API] with a delay of [25] seconds [after] the call,
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-004
  Scenario: must get an error response for a document id which is not associated
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   another successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And   another successful call [to create a case of this case type] as in [S-004_Case_Create]
    And   a user with [an active solicitor1 profile in CCD with no READ permission on the document field],
    When  a request is prepared with appropriate values,
    And   the request [contains the id of the document just uploaded above],
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-005
  Scenario: generic scenario for Unauthorized

  @S-006
  Scenario: generic scenario for Forbidden

  @S-007
  Scenario: generic scenario for Unsupported Media Type

  @S-110 @Ignore #this scenario is not valid anymore after CCD-3138.
  Scenario: must get an error response when CCD Data Store tries to access Get Document Metadata API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When  a request is prepared with appropriate values,
    And   the request [is to be made on behalf of CCD Data Store API],
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-111
  Scenario: must get an error response when Bulk Scan Processor tries to access Get Document Metadata API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When  a request is prepared with appropriate values,
    And   the request [is to be made on behalf of Bulk Scan Processor API],
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-112 #This can be enabled once ccd_gw level permissions are removed.
  Scenario: must get an error response when API-Gateway tries to access Get Document Metadata API
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#    When  a request is prepared with appropriate values,
#    And   the request [is to be made on behalf of API-Gateway API],
#    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API],
#    Then  a negative response is received,
#    And   the response has all the details as expected.

  @S-001.13
  Scenario: Document meta-data exists in Doc-store without Case Id; with a valid TTL (in future), Jurisdiction and
            case type matching the service identifier supplied in the Get request- Happy Path 1
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   another successful call [to set the TTL of the metadata for the uploaded document to a future date] as in [Patch_Document_Ttl_Future_Date],
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API],
    Then  a positive response is received,
    And   the response [contains the metadata for the document uploaded above],
    And   the response has all other details as expected.

  @S-001.14
  Scenario:  Document meta-data exists in Doc-store without case id; with a invalid TTL (in the past)- Error Path 1
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And   another successful call [to set the TTL of the metadata for the uploaded document to a past date] as in [Patch_Document_Ttl_Past_Date],
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API],
    Then  a negative response is received
    And   the response has all other details as expected.

