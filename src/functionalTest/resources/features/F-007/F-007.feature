@F-007
Feature: F-007: Attach Documents to Case

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-070
  Scenario: must successfully validate hash token based on the cached values.
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [contains Case Type Id, Jurisdiction Id, Document Id and Hash token which are received from Get Document Metadata by Document ID]
    And the request [contains Case Id]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a positive response is received
    And the response has all other details as expected

  @S-071
  Scenario: must get an error response for a non existing hash token
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [for a non existing hash token]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected

  @S-072
  Scenario: must get an error response for a non existing document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [for a non existing document Id]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected

  @S-073
  Scenario: must get an error response for a malformed document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [for a malformed document Id]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected

  @S-074
  Scenario: must get an error response for a non existing CaseType Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [for a non existing CaseType Id]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected

  @S-075
  Scenario: must get an error response for a malformed CaseType Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [for a malformed CaseType Id]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected

  @S-076
  Scenario: must get an error response for a non existing Jurisdiction Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [for a non existing Jurisdiction Id]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected

  @S-077
  Scenario: must get an error response for a malformed Jurisdiction Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [for a malformed Jurisdiction Id]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected

  @S-078
  Scenario: must get an error response for a malformed Case Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    And another successful call [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    When a request is prepared with appropriate values
    And the request [for a malformed Case Id]
    And it is submitted to call the [Attach Document To Case] operation of [CCD Case Document AM API]
    Then a negative response is received
    And the response has all the details as expected
