@F-003
Feature: F-003: Delete Document by Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-031
  Scenario: must successfully delete a document by document Id permanent delete
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
    Then a positive response is received,
    And the response has all other details as expected,
    And another call [to Get the Document just deleted] will get the expected response as in [F-003_Get_Document_with_404]

  @S-032
  Scenario: must successfully delete a document by document Id soft delete
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
    Then a positive response is received,
    And the response has all other details as expected.
    And another call [to Get the Document just deleted] will get the expected response as in [F-003_Get_Document_with_404]

  @S-033 #Need to raise a bug with DM team as actual DM store API is not returning 404 code.
  Scenario: must get an error response for a non-existing document Id
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#    When a request is prepared with appropriate values,
#    And the request [contains a non-existing document Id],
#    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
#    Then a negative response is received,
#    And the response has all the details as expected.

  @S-034
  Scenario: must get an error response for a malformed document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When a request is prepared with appropriate values,
    And the request [contains a malformed document Id],
    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-035
  Scenario: generic scenario for Unauthorized

  @S-036
  Scenario: generic scenario for Forbidden

  @S-037
  Scenario: generic scenario for Unsupported Media Type

  @S-126
  Scenario: must get an error response when CCD Data Store tries to access Delete Document API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-127
  Scenario: must get an error response when Bulk Scan Processor tries to access Delete Document API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [is to be made on behalf of Bulk Scan Processor API],
    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-128 #This can be enabled once ccd_gw level permissions are removed.
  Scenario: must get an error response when API-Gateway tries to access Delete Document API
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#    When a request is prepared with appropriate values,
#    And the request [is to be made on behalf of API-Gateway API],
#    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
#    Then a negative response is received,
#    And the response has all the details as expected.
