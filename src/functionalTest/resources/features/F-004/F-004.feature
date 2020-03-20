@F-004
Feature: F-004: Post Upload Document with Binary Content

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-040
  Scenario: must successfully Post Upload Document with Binary Content
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a positive response is received
    And   the response [contains the metadata for the document uploaded above]
    And   the response has all other details as expected

  @S-041
  Scenario: must get an error response for a malformed caseTypeId
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed caseTypeId]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-042
  Scenario: must get an error response for a without caseTypeId parameter in request
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [without caseTypeId parameter]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-043
  Scenario: must get an error response for a malformed jurisdictionId
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed jurisdictionId]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-044
  Scenario: must get an error response for without jurisdictionId parameter in request
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [without jurisdictionId parameter]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-045
  Scenario: must get an error response for a non-existing classification
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a non-existing classification]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-046
  Scenario: must get an error response for a malformed classification
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed classification]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-047
  Scenario: must get an error response for a malformed roles
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed roles]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-048
  Scenario: must successfully Post Upload multiple Document with Binary Content
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contain multiple document with binary content]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a positive response is received1
    And   the response [contains the metadata for the document uploaded above]
    And   the response has all other details as expected

#  @S-049 @Ignore # This is the test need to be done manually
#  Scenario: must get an error response for a above max allowed size of a document
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    When  a request is prepared with appropriate values
#    And   the request [for a above max allowed size of a document]
#    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
#    Then  a negative response is received
#    And   the response has all the details as expected

#  @S-050 @Ignore # This is the test need to be done manually
#  Scenario: must get an error response for a upload document with unauthorised user id after 10 minuts
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    When  a request is prepared with appropriate values
#    And   the request [contains unauthorised user id]
#    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
#    Then  a positive response is received
#    And   the response has all the details as expected
#    And   the request [contains the id of the document just uploaded above and wait for 10 minuts]
#    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
#    Then  a negative response is received
#    And   the response has all the details as expected

  @S-051 # should be picked up automatically by the freamework as suggested by CCD team
  Scenario: generic scenario for Unauthorized

  @S-052 # should be picked up automatically by the freamework as suggested by CCD team
  Scenario: generic scenario for Forbidden

  @S-053 # should be picked up automatically by the freamework as suggested by CCD team
  Scenario: generic scenario for Unsupported Media Type
