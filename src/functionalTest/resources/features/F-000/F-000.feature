@F-000
Feature: F-000: Upload case document to DM Store

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-000 @Ignore
  Scenario: [SAMPLE] must retrieve case document metadata successfully
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When a request is prepared with appropriate values
    And it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    Then a positive response is received
    And the response has all the details as expected


