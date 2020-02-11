@F-001
Feature: F-001: Retrieval of metadata from doc-store for given document id

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-001
  Scenario: Successful retrieval of metadata from doc-store for given document id
    Given a user with [an active profile in CCD]
    When  a request is prepared with appropriate values
    And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
    Then  a positive response is received
    And   the response [contains the case detail for the updated case, along with an HTTP 200 OK]
    And   the response has all other details as expected

   @S-002
   Scenario: Successful retrieval of metadata from doc-store for given document id with optional fields
     Given a user with [an active profile in CCD]
     When  a request is prepared with appropriate values
     And   the request[contains valid data for optional fields i.e. userId and userRoles]
     And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
     Then  a positive response is received
     And   the response [contains the case detail for the updated case, along with an HTTP 200 OK]
     And   the response has all other details as expected

   @S-003
   Scenario: Successful retrieval of metadata from doc-store for given document id with malformed data in the optional field
     Given a user with [an active profile in CCD]
     When  a request is prepared with appropriate values
     And   the request[contains malformed data for optional fields i.e. userId and userRoles]
     And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD data store]
     Then  a positive response is received
     And   the response [contains the case detail for the updated case, along with an HTTP 200 OK]
     And   the response has all other details as expected

   @S-004
   Scenario: must return a negative response 401 when the request does provide unauthorized credentials
     Given a user with [an active profile in CCD]
     When  a request is prepared with appropriate values
     And   the request [*does provide unauthorized credentials*]
     And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
     Then  a negative response is received
     And   the response [contains an HTTP 401 'Unauthorized']
     And   the response has all other details as expected

   @S-005
   Scenario: must return a negative response 403 when request provides authentic credentials without authorised access
     Given a user with [an active profile in CCD]
     When  a request is prepared with appropriate values
     And   the request [user provides a wrong service Authorisation token]
     And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
     Then  a negative response is received
     And   the response [contains an HTTP 403 'Forbidden']
     And   the response has all other details as expected

   @S-006
   Scenario: Receive an error response, for retrieval of doc-store metadata for non-existing document id
     Given a user with [an active profile in CCD]
     When  a request is prepared with appropriate values
     And   the request [for retrieval of retrieval doc-store metadata for non-existing document id]
     And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
     Then  a negative response is received
     And   the response [contains an HTTP 404 'Not Found']
     And   the response has all the details as expected

   @S-007
   Scenario: Receive an error response, for retrieval of doc-store metadata for document id which is not associated
     Given a user with [an active profile in CCD]
     When  a request is prepared with appropriate values
     And   the request [for retrieval *of doc-store metadata for document id which is not associated*]
     And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
     Then  a negative response is received
     And   the response [contains an HTTP 404 'Not Found']
     And   the response has all the details as expected

   @S-008
   Scenario: Receive an error response, for retrieval of doc-store metadata for malformed document id
     Given a user with [an active profile in CCD]
     When  a request is prepared with appropriate values
     And   the request [for retrieval of doc-store metadata for malformed document id]
     And   it is submitted to call the [Retrieval of metadata from doc-store for given document id] operation of [CCD data store]
     Then  a negative response is received
     And   the response [contains an HTTP 404 'Not Found']
     And   the response has all the details as expected

   @S-009
   Scenario: must return a negative response when the request body doesn't provide a mandatory field document id
       Given a user with [an active profile in CCD]
       When  a request is prepared with appropriate values
       And   the request [body doesn't provide a mandatory field document id ]
       And   it is submitted to call the [retrieval of metadata from doc-store for given document id] operation of [CCD Data Store]
       Then  a negative response is received
       And   the response [has the 400 return code]
       And   the response has all other details as expected
