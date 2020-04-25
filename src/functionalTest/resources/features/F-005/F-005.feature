@F-005
Feature: F-005: Patch Document with ttl

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-051
  Scenario: must successfully Patch Document with ttl
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When  a request is prepared with appropriate values,
    And   the request [contains document id uploaded above with ttl],
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
    Then  a positive response is received,
    And   the response [contains the same ttl applied in request],
    And   the response has all other details as expected.
#
  @S-052
  Scenario: must get an error response for a non-existing ttl
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When  a request is prepared with appropriate values,
    And   the request [contains a non-existing ttl],
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-053
  Scenario: must get an error response for a malformed ttl
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When  a request is prepared with appropriate values,
    And   the request [contains a malformed ttl],
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-054
  Scenario: must get an error response for a malformed document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains a malformed document Id],
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-055
  Scenario: must get an error response for a non-existing document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains a non-existing document Id],
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

# Below tests are invalid and need to be replaced with service level authorisations.
#  @S-056 @Ignore #this test has to be done manually
#  Scenario: must get an error response for patch document with ttl for unprivileged user
#    Given a user with [an active caseworker profile in CCD with unprivileged user (i.e. just Create permission) on a document field],
#    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload],
#    When  a request is prepared with appropriate values,
#    And   the request [contains unprivileged user and a document id uploaded above with ttl],
#    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
#    Then  a negative response is received,
#    And   the response has all other details as expected.
#
#  @S-057 @Ignore #this test has to be done manually
#  Scenario: must get an error response for patch document with ttl for unprivileged user
#    Given a user with [an active caseworker profile in CCD with unprivileged user (i.e. just Read permission) on a document field],
#    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload],
#    When  a request is prepared with appropriate values,
#    And   the request [contains unprivileged user and a document id uploaded above with ttl],
#    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
#    Then  a negative response is received,
#    And   the response has all other details as expected.
#
#  @S-057 @Ignore #this test has to be done manually
#  Scenario: must get an error response for patch document with ttl for unprivileged user
#    Given a user with [an active caseworker profile in CCD with unprivileged user (i.e. just Update permission) on a document field],
#    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload],
#    When  a request is prepared with appropriate values,
#    And   the request [contains unprivileged user and a document id uploaded above with ttl],
#    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
#    Then  a positive response is received,
#    And   the response has all other details as expected.
#
  @S-057
  Scenario: generic scenario for Unauthorized

  @S-058
  Scenario: generic scenario for Forbidden

  @S-059
  Scenario: generic scenario for Unsupported Media Type

  @S-122
  Scenario: must get an error response when CCD Data Store tries to access Patch Document with ttl API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When  a request is prepared with appropriate values,
    And   the request [contains document id uploaded above with ttl],
    And   the request [is to be made on behalf of CCD Data Store API],
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-123
  Scenario: must get an error response when Bulk Scan Processor tries to access Patch Document with ttl API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When  a request is prepared with appropriate values,
    And   the request [contains document id uploaded above with ttl],
    And   the request [is to be made on behalf of Bulk Scan Processor API],
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-124
  Scenario: must get an error response when API-Gateway tries to access Patch Document with ttl API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When  a request is prepared with appropriate values,
    And   the request [contains document id uploaded above with ttl],
    And   the request [is to be made on behalf of API-Gateway API],
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.
