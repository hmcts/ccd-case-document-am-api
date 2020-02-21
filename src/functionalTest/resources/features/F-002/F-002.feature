@F-002
Feature: F-002: Get Document's Binary Content from Doc-Store for a given document id

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-020
    Scenario: must successfully receive a document's binary content
      Given a user with [an active caseworker profile in CCD with full permissions on a document field]
      And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
      When a request is prepared with appropriate values
      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document]
      Then a positive response is received
      And the response [contains the binary content for the uploaded document]

    @S-021
    Scenario: must successfully receive a document's binary for a null user id
      Given a user with [an active caseworker profile in CCD with full permissions on a document field]
      And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
      When a request is prepared with appropriate values
      And the request [contains a null user id]
      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document]
      Then a positive response is received
      And the response [contains the binary content for the uploaded document]

    @S-022
    Scenario: must successfully receive a document's binary content for a wrong user id
      Given a user with [an active caseworker profile in CCD with full permissions on a document field]
      And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
      When a request is prepared with appropriate values
      And the request [contains a wrong user id]
      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document]
      Then a positive response is received
      And the response [contains the binary content for the uploaded document]

    @S-023
    Scenario: must receive an error response for a non existing document id
      Given a user with [an active caseworker profile in CCD with full permissions on a document field]
      When a request is prepared with appropriate values
      And the request [contains a non existing document id]
      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document]
      Then a negative response is received
      And the response has all other details as expected

    @S-024
    Scenario: must receive an error response for a non existing user-role
      Given a user with [an active caseworker profile in CCD with full permissions on a document field]
      When a request is prepared with appropriate values
      And the request [contains a non existing case-role]
      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document]
      Then a negative response is received
      And the response has all other details as expected

    #Generic Scenarios for Security
    @S-025
    Scenario: generic scenario for Unauthorized

    @S-026
    Scenario: generic scenario for Forbidden

    @S-027
    Scenario: generic scenario for Unsupported Media Type
