@F-002
Feature: F-002: Get Document Binary Content by Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source
#
#    @S-020
#    Scenario: must successfully receive a document's binary content
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
#      Then a positive response is received,
#      And the response [contains the binary content for the uploaded document],
#      And the response has all other details as expected.
#
#    @S-021
#    Scenario: must receive an error response for a non existing document id
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      When a request is prepared with appropriate values,
#      And the request [contains a non existing document id],
#      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
#      Then a negative response is received,
#      And the response has all other details as expected.
#
#    @S-022 @Ignore #will test this scenario after AM-450 implementation
#    Scenario: must receive an error response for an active caseworker who does not have document access
#      Given a user with [an active caseworker profile in CCD with limited permissions on a document field],
#      And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [contains an active caseworker who does not have document access],
#      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
#      Then a negative response is received,
#      And the response has all other details as expected.
#
#    @S-023
#    Scenario: must receive an error response for a malformed document ID
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      When a request is prepared with appropriate values,
#      And the request [contains a malformed document ID],
#      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#
#    #Generic Scenarios for Security
#    @S-024 @Ignore
#    Scenario: generic scenario for Unauthorized
#
#    @S-025 @Ignore
#    Scenario: generic scenario for Forbidden
#
#    @S-026 @Ignore
#    Scenario: generic scenario for Unsupported Media Type

  @S-114
  Scenario: must receive an error response when CCD Data Store tries to access Get Document Binary Content by Document ID API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-115
  Scenario: must receive an error response when Bulk Scan Processor tries to access Get Document Binary Content by Document ID API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [is to be made on behalf of Bulk Scan Processor API],
    And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-116
  Scenario: must receive an error response when API-Gateway tries to access Get Document Binary Content by Document ID API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And the request [is to be made on behalf of API-Gateway API],
    And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.
