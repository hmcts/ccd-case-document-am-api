@F-007
Feature: F-007: Attach Document to Case

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-070
  Scenario: must successfully attach a document to a case with hash token
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a Case Id],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a positive response is received,
    And the response has all other details as expected.

  @S-071
  Scenario: must get an error response for a wrong hash token
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a wrong hash token],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-072
  Scenario: must get an error response for a non existing document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a non existing document Id],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-073
  Scenario: must get an error response for a malformed document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a malformed document Id],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-075
  Scenario: must get an error response for a malformed CaseType Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a malformed CaseType Id],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-077
  Scenario: must get an error response for a malformed Jurisdiction Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a malformed Jurisdiction Id],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-078
  Scenario: must get an error response for a malformed Case Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a malformed Case Id],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-079
  Scenario: must successfully validate hashed-token for multiple documents
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document2],
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document3],
    When a request is prepared with appropriate values,
    And the request [contains a Case Id along with ids of the documents uploaded above],
    And the request [is to be made on behalf of CCD Data Store API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a positive response is received,
    And the response has all other details as expected.

  @S-080 @Ignore
  Scenario: generic scenario for Unauthorised

  @S-081 @Ignore
  Scenario: generic scenario for Forbidden

  @S-102
  Scenario: must get an error response when Ex-UI tries to access Attach Document to Case API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a Case Id],
    And the request [is to be made on behalf of Ex-UI API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-103
  Scenario: must get an error response when Bulk Scan Processor tries to access Attach Document to Case API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a Case Id],
    And the request [is to be made on behalf of Bulk Scan Processor API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

  @S-104
  Scenario: must get an error response when API-Gateway tries to access Attach Document to Case API
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
    When a request is prepared with appropriate values,
    And the request [contains a Case Id],
    And the request [is to be made on behalf of API-Gateway API],
    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.
