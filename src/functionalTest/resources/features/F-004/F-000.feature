@F-004
Feature: F-004: Post Upload Document with Binary Content

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-041
  Scenario: must successfully Post Upload Document with Binary Content
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a positive response is received
    And   the response [contains the metadata for the document uploaded above]
    And   the response has all other details as expected

  @S-042
  Scenario: must get an error response for a non-existing caseTypeId
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a non-existing caseTypeId]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-043
  Scenario: must get an error response for a malformed caseTypeId
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed caseTypeId]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-044
  Scenario: must get an error response for a non-existing jurisdictionId
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a non-existing jurisdictionId]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-045
  Scenario: must get an error response for a malformed jurisdictionId
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed jurisdictionId]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-046
  Scenario: must get an error response for a non-existing classification
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a non-existing classification]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-047
  Scenario: must get an error response for a malformed classification
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed classification]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-048
  Scenario: must get an error response for a non-existing roles
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a non-existing roles]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-049
  Scenario: must get an error response for a malformed roles
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed roles]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-050
  Scenario: must get an error response for a non-existing roles
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a non-existing roles]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-051
  Scenario: must get an error response for a malformed roles
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed roles]
    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-052
  Scenario: must get an error response for a upload document with unauthorised user id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When a request is prepared with appropriate values
    And the request [contains unauthorised user id]
    And it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected

  @S-053
  Scenario: generic scenario for Unauthorized

  @S-054
  Scenario: generic scenario for Forbidden

  @S-055
  Scenario: generic scenario for Unsupported Media Type
