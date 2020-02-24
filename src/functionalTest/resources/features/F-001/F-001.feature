@F-001
Feature: F-001: Get Document Metadata by Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-001
  Scenario: must successfully get document metadata by document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    And   a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
    When  a request is prepared with appropriate values
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    Then  a positive response is received
#    And   the response [contains the metadata for the document uploaded above]
    And   the response has all other details as expected

  @S-002
  Scenario: must get an error response for a non-existing document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a non-existing document ID]
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

  @S-003
  Scenario: must get an error response for a malformed document ID
    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
    When  a request is prepared with appropriate values
    And   the request [for a malformed document ID]
    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
    Then  a negative response is received
    And   the response has all the details as expected

#  @S-004 # Waiting from Mutlu review comment on this
#  Scenario: must get an error response for a document id which is not associated
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field]
#    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
#    And   a user with [an active citizen profile in CCD with full permission on specific document field]
#    And   a successful call [by a privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload]
#    When  a request is prepared with appropriate values
#    And   the request [for retrieval of doc-store metadata for document id uploaded by citizen]
#    And   it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
#    Then  a negative response is received
#    And   the response has all the details as expected


  @S-005
  Scenario: generic scenario for Unauthorized

  @S-006
  Scenario: generic scenario for Forbidden

  @S-007
  Scenario: generic scenario for Unsupported Media Type


