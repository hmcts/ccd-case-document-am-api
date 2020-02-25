@F-000
Feature: [SAMPLE] Get default settings for user

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-000
  Scenario: [SAMPLE] must return default user setting successfully for a user having a profile in CCD
    Given a successful call [to retrieve the metadata by document id] as in [S-000-Upload]
    And another successful call [to retrieve the metadata by document id] as in [S-000-GetMetadata]
    And a user with [a detailed profile in CCD]
    And a case that has just been created as in [Befta_Default_Full_Case_Creation_Data]
    When a request is prepared with appropriate values
    And the request [uses a uid that exists in IDAM]
    And it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    Then a positive response is received
    And the response has all the details as expected

