@F-004
Feature: F-004: Upload Document With Binary Content

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-040
  Scenario: must successfully upload document with binary content
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a positive response is received,
    And   the response [contains the metadata for the document uploaded above],
    And   the response has all other details as expected.

  @S-048
  Scenario: must successfully upload multiple document with binary content
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains multiple documents with binary contents],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a positive response is received,
    And   the response [contains the metadata for each of the documents uploaded above],
    And   the response has all other details as expected.

  @S-041
  Scenario: must get an error response for a malformed case type id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains a malformed case type id],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-042
  Scenario: must get an error response for a without case type id parameter in request
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains without case type id parameter],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-043
  Scenario: must get an error response for a malformed jurisdiction Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains a malformed jurisdiction Id],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-044
  Scenario: must get an error response for without jurisdiction Id parameter in request
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains without jurisdiction Id parameter],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-045
  Scenario: must get an error response for a non-existing security classification
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains a non-existing security classification],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-046
  Scenario: must get an error response for a malformed security classification
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [contains a malformed security classification],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

#  @S-047 @Ignore # Not required
#  Scenario: must get an error response for a malformed roles
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#    When  a request is prepared with appropriate values,
#    And   the request [contains a malformed roles],
#    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
#    Then  a negative response is received,
#    And   the response has all the details as expected.
#
#  @S-049 @Ignore # This is the test need to be done manually
#  Scenario: must get an error response for a above max allowed size of a document
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#    When  a request is prepared with appropriate values,
#    And   the request [contains a max allowed size of a document],
#    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
#    Then  a negative response is received,
#    And   the response has all the details as expected.
#
#  @S-050 @Ignore # This is the test need to be done manually
#  Scenario: must get an error response for a upload document with unauthorised user id after 10 minuts
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#    When  a request is prepared with appropriate values,
#    And   the request [contains unauthorised user],
#    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
#    Then  a positive response is received,
#    And   the response has all the details as expected,
#    And   the request [contains the id of the document just uploaded above and wait for 10 minutes],
#    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API],
#    Then  a negative response is received,
#    And   the response has all the details as expected.

  @S-051 # should be picked up automatically by the freamework as suggested by CCD team
  Scenario: generic scenario for Unauthorized

  @S-052 # should be picked up automatically by the freamework as suggested by CCD team
  Scenario: generic scenario for Forbidden

  @S-053 # should be picked up automatically by the freamework as suggested by CCD team
  Scenario: generic scenario for Unsupported Media Type

  @S-118
  Scenario: must get an error response when CCD Data Store tries to access Upload Document With Binary Content API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [is to be made on behalf of CCD Data Store API],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-119
  Scenario: must get an error response when Bulk Scan Processor tries to access Upload Document With Binary Content API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [is to be made on behalf of Bulk Scan Processor API],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-120
  Scenario: must get an error response when API-Gateway tries to access Upload Document With Binary Content API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When  a request is prepared with appropriate values,
    And   the request [is to be made on behalf of API-Gateway API],
    And   it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API],
    Then  a negative response is received,
    And   the response has all the details as expected.

