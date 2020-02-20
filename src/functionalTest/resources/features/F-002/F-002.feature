@F-002
Feature: F-002: Retrieval of document's binary content from Doc-store for a given document id

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

    @S-020
    Scenario: must receive a document's binary content, when document download is authorised
      Given a user with [an active profile in CCD]
      And a user with [an active caseworker profile in CCD with full permissions on a document field]
      And a successful call [by a privileged user to upload a document] as in [Default_Document_Upload_Data]
      When a request is prepared with appropriate values
      And it is submitted to call the [get binary content from doc-store] operation of [CCD case document]
      Then a positive response is received
      And the response [contains the binary content for the uploaded document, along with an HTTP 200 OK]

    @S-021
    Scenario: must receive a document's binary content eventhough user id is not provided
      Given a user with [an active profile in CCD]
      And a user with [an active caseworker profile in CCD with full permissions on a document field]
      And a successful call [by a privileged user to upload a document] as in [Default_Document_Upload_Data]
      When a request is prepared with appropriate values
      And the request [contains null user id]
      And it is submitted to call the [get binary content from doc-store] operation of [CCD case document]
      Then a positive response is received
      And the response [contains the binary content for the uploaded document, along with an HTTP 200 OK]

    @S-022
    Scenario: must receive a document's binary content when wrong user id provided
      Given a user with [an active profile in CCD]
      And a user with [an active caseworker profile in CCD with full permissions on a document field]
      And a successful call [by a privileged user to upload a document] as in [Default_Document_Upload_Data]
      When a request is prepared with appropriate values
      And the request [contains a wrong user id]
      And it is submitted to call the [get binary content from doc-store] operation of [CCD case document]
      Then a positive response is received
      And the response [contains the binary content for the uploaded document, along with an HTTP 200 OK]

    @S-023
    Scenario: must receive an error response when document id does not exist
      Given a user with [an active profile in CCD]
      And a user with [an active caseworker profile in CCD with full permissions on a document field]
      When a request is prepared with appropriate values
      And the request [has a document id which does not exist]
      And it is submitted to call the [get binary content from doc-store] operation of [CCD case document]
      Then a negative response is received
      And the response [contains an HTTP 404 status code]
      And the response has all other details as expected

    @S-024
    Scenario: must receive an error response when request provides without valid serviceAuthorisation
      Given a user with [an active profile in CCD]
      And a user with [an active caseworker profile in CCD with full permissions on a document field]
      And a successful call [by a privileged user to upload a document] as in [Default_Document_Upload_Data]
      When a request is prepared with appropriate values
      And the request [contains an invalid s2s authorisation token]
      And it is submitted to call the [get binary content from doc-store] operation of [CCD case document]
      Then a negative response is received
      And the response [contains an HTTP 403 Access Denied]
      And the response has all other details as expected

    @S-025
    Scenario: must receive an error response when request provides expired s2s serviceAuthorisation
      Given a user with [an active profile in CCD]
      And a user with [an active caseworker profile in CCD with full permissions on a document field]
      And a successful call [by a privileged user to upload a document] as in [Default_Document_Upload_Data]
      When a request is prepared with appropriate values
      And the request [contains an expired s2s serviceAuthorisation token]
      And it is submitted to call the [get binary content from doc-store] operation of [CCD case document]
      Then a negative response is received
      And the response [contains an HTTP 401 Unauthorised]
      And the response has all other details as expected

  @S-026
  Scenario: must receive an error response when user-roles does not exist
    Given a user with [an active profile in CCD]
    And a user with [an active caseworker profile in CCD with full permissions on a document field]
    When a request is prepared with appropriate values
    And the request [has a case-role which does not exist]
    And it is submitted to call the [get binary content from doc-store] operation of [CCD case document]
    Then a negative response is received
    And the response [contains an HTTP 404 status code]
    And the response has all other details as expected

  @S-027
  Scenario: must receive an error response when request provides content type other than application/json
    Given a user with [an active profile in CCD]
    And a user with [an active caseworker profile in CCD with full permissions on a document field]
    When a request is prepared with appropriate values
    And the request [contains a content type header of application/xml]
    And it is submitted to call the [get binary content from doc-store] operation of [CCD case document]
    Then a negative response is received
    And the response [contains an HTTP 415 Unsupported Media Type]
    And the response has all other details as expected

