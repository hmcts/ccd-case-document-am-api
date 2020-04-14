@F-006
Feature: F-006: Get hashtoken by Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

#  @S-061
#  Scenario: must successfully get hashtoken by document Id
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
#    When a request is prepared with appropriate values
#    And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API]
#    Then a positive response is received
#    And the response has all other details as expected
#
#  @S-062
#  Scenario: must get an error response for a malformed document ID
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    When a request is prepared with appropriate values
#    And the request [contains a malformed document ID]
#    And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API]
#    Then a negative response is received
#    And the response has all the details as expected
#
#  @S-063
#  Scenario: must get an error response for a malformed caseType
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    When  a request is prepared with appropriate values
#    And   the request [for a malformed caseTypeId]
#    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
#    Then  a negative response is received
#    And   the response has all the details as expected
#
#  @S-064
#  Scenario: must get an error response for a without caseType parameter in request
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    When  a request is prepared with appropriate values
#    And   the request [without caseTypeId parameter]
#    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
#    Then  a negative response is received
#    And   the response has all the details as expected
#
#  @S-065
#  Scenario: must get an error response for a malformed jurisdictionId
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    When  a request is prepared with appropriate values
#    And   the request [for a malformed jurisdictionId]
#    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
#    Then  a negative response is received
#    And   the response has all the details as expected
#
#  @S-066
#  Scenario: must get an error response for without jurisdictionId parameter in request
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    When  a request is prepared with appropriate values
#    And   the request [without jurisdictionId parameter]
#    And   it is submitted to call the [Post Upload Document with Binary Content] operation of [CCD Case Document AM API]
#    Then  a negative response is received
#    And   the response has all the details as expected


  @S-067
  Scenario: generic scenario for Unauthorized

  @S-068
  Scenario: generic scenario for Forbidden

  @S-069
  Scenario: generic scenario for Unsupported Media Type
