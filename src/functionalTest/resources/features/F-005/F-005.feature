@F-005
Feature: F-005: Patch Document with ttl

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-051
  Scenario: must successfully Patch Document with ttl
    Given a user with [an active caseworker profile in CCD with limited permissions on a document field]
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    When  a request is prepared with appropriate values
    And   the request [contains document id uploaded above with ttl]
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
    Then  a positive response is received
    And   the response [contains the same ttl and document ID uploaded above]
    And   the response has all other details as expected

  @S-052
  Scenario: must get an error response for a non-existing ttl
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains a non-existing ttl]
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-053
  Scenario: must get an error response for a malformed ttl
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [contains a malformed ttl]
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-054 @Ignore #this test has to be done manually
  Scenario: must get an error response for patch document with ttl for unprivileged user
    Given a user with [an active caseworker profile in CCD with unprivileged user (i.e. just Create permission) on a document field]
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    When  a request is prepared with appropriate values
    And   the request [contains unprivileged user and a document id uploaded above with ttl]
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all other details as expected

  @S-055 @Ignore #this test has to be done manually
  Scenario: must get an error response for patch document with ttl for unprivileged user
    Given a user with [an active caseworker profile in CCD with unprivileged user (i.e. just Read permission) on a document field]
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    When  a request is prepared with appropriate values
    And   the request [contains unprivileged user and a document id uploaded above with ttl]
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all other details as expected

  @S-056 @Ignore #this test has to be done manually
  Scenario: must get an error response for patch document with ttl for unprivileged user
    Given a user with [an active caseworker profile in CCD with unprivileged user (i.e. just Update permission) on a document field]
    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    When  a request is prepared with appropriate values
    And   the request [contains unprivileged user and a document id uploaded above with ttl]
    And   it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
    Then  a positive response is received
    And   the response has all other details as expected

  @S-057
  Scenario: generic scenario for Unauthorized

  @S-058
  Scenario: generic scenario for Forbidden

  @S-059
  Scenario: generic scenario for Unsupported Media Type
