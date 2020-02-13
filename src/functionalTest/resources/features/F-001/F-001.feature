@F-001
Feature: F-001: Retrieval of metadata from doc-store for given document id

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-001
  Scenario: Successful retrieval of metadata from doc-store for given document id for caseworker
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [S-001-Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a positive response is received
    And   the response [contains the metadata for the document uploaded above, along with an HTTP 200 OK]
    And   the response has all other details as expected

  @S-002
  Scenario: Successful retrieval of metadata from doc-store for given document id with optional fields linked with a serviceAuthorization token
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request[contains valid data for optional fields i.e. serviceAuthorization token]
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a positive response is received
    And   the response [contains the case detail for the updated case, along with an HTTP 200 OK]
    And   the response has all other details as expected

  @S-003
  Scenario: Successful retrieval of metadata from doc-store for given document id with optional fields not linked with a serviceAuthorization token
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request[contains valid data for optional fields not linked with a serviceAuthorization token]
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a positive response is received
    And   the response [contains the case detail for the updated case, along with an HTTP 200 OK]
    And   the response has all other details as expected

  @S-004
  Scenario: Successful retrieval of metadata from doc-store for given document id with optional fields non-existing serviceAuthorization token
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request[contains valid data for optional fields non-existing serviceAuthorization token]
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a positive response is received
    And   the response [contains the case detail for the updated case, along with an HTTP 200 OK]
    And   the response has all other details as expected

  @S-005
  Scenario: Successful retrieval of metadata from doc-store for given document id without optional field
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request[without optional field]
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a positive response is received
    And   the response [contains the case detail for the updated case, along with an HTTP 200 OK]
    And   the response has all other details as expected

  @S-006
  Scenario: Successful retrieval of metadata from doc-store for given document id with malformed data in the optional field
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request[contains malformed data malformed data in the optional field]
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a positive response is received
    And   the response [contains the case detail for the updated case, along with an HTTP 200 OK]
    And   the response has all other details as expected

  @S-007
  Scenario: must return a negative response 401 when the request does provide unauthorized credentials
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request [does provide unauthorized credentials*]
    And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a negative response is received
    And   the response [contains an HTTP 401 'Unauthorized']
    And   the response has all other details as expected

  @S-008
  Scenario: must return a negative response 403 when request provides authentic credentials without authorised access
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request [user provides a wrong service Authorisation token]
    And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a negative response is received
    And   the response [contains an HTTP 403 'Forbidden']
    And   the response has all other details as expected

  @S-009
  Scenario: Receive an error response, for retrieval of doc-store metadata for non-existing document id
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request [for retrieval of retrieval doc-store metadata for non-existing document id]
    And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a negative response is received
    And   the response [contains an HTTP 404 'Not Found']
    And   the response has all the details as expected

  @S-010
  Scenario: Receive an error response, for retrieval of doc-store metadata for document id which is not associated
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    And   a user with [an active citizen profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request [for retrieval of doc-store metadata for document id uploaded by citizen]
    And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a negative response is received
    And   the response [contains an HTTP 403 'Forbidden']
    And   the response has all the details as expected

  @S-011
  Scenario: Receive an error response, for retrieval of doc-store metadata for malformed document id
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request [for retrieval of doc-store metadata for malformed document id]
    And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a negative response is received
    And   the response [contains an HTTP 400 'Not Found']
    And   the response has all the details as expected

  @S-012
  Scenario: must return a negative response when the request body doesn't provide a mandatory field document id
    Given a user with [an active profile in CCD]
    And   a user with [an active caseworker profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   the request [body doesn't provide a mandatory field document id ]
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD Data Store]
    Then  a negative response is received
    And   the response [has the 400 return code]
    And   the response has all other details as expected

  @S-013
  Scenario: Successful retrieval of metadata from doc-store for given document id for citizen
    Given a user with [an active profile in CCD]
    And   a user with [an active citizen profile in CCD with full permission on specific document field]
    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data]
    When  a request is prepared with appropriate values
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a positive response is received
    And   the response [contains the metadata for the document uploaded above, along with an HTTP 200 OK]
    And   the response has all other details as expected
