@F-006
Feature: F-006: Get hashtoken by Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-061
  Scenario: must successfully get hashtoken by document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [contains the document Id from just uploaded document],
    And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API],
    Then a positive response is received,
    And the response [contains the hashtoken received from just uploaded document],
    And the response has all other details as expected.

  @S-062
  Scenario: must get an error response for a malformed document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When a request is prepared with appropriate values,
    And the request [contains a malformed document Id],
    And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-063
  Scenario: must get an error response for a non existing document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And the request [contains a non existing document Id],
    And   it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-064
  Scenario: must successfully get an updated hashtoken after the caseId attached on document metadata
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    And another successful call [by same user to get a hashtoken] as in [S-064_Get_Hash_Token],
    And another successful call [by same user to attach this documents to a case] as in [S-064_Attach_Case_Id],
    When a request is prepared with appropriate values,
    And the request [contains the document Id from just uploaded document],
    And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API],
    Then a positive response is received,
    And the response [doesn't contain the hashtoken received from previous call],
    And the response has all other details as expected.

  @S-064
  Scenario: generic scenario for Unauthorized

  @S-065
  Scenario: generic scenario for Forbidden

  @S-066
  Scenario: generic scenario for Unsupported Media Type

  @S-106
  Scenario: must get an error response when Ex-UI tries to access get hashtoken API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [contains the document Id from just uploaded document],
    And the request [is to be made on behalf of Ex-UI API],
    And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-107
  Scenario: must get an error response when API-Gateway tries to access get hashtoken API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [contains the document Id from just uploaded document],
    And the request [is to be made on behalf of API-Gateway API],
    And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-108
  Scenario: must get an error response when CCD Data Store tries to access get hashtoken API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [contains the document Id from just uploaded document],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.
