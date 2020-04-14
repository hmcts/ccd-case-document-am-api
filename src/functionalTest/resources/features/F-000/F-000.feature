@F-000
Feature: [SAMPLE] Upload case document to DM Store

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-000
  Scenario: [SAMPLE] must upload a document successfully and get the response
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When a request is prepared with appropriate values
    And the request [uses a uid that exists in IDAM]
    And it is submitted to call the [upload a document with mandatory metadata] operation of [CCD Case Document AM API]
    Then a positive response is received
    And the response has all the details as expected

