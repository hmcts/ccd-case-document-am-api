@F-003
Feature: F-003: Delete Document by Document ID

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-031
  Scenario: must successfully delete a document by document Id permanent delete
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
    Then a positive response is received,
    And the response has all other details as expected,
#    And another call [to Get the Document just deleted] will get the expected response as in [Get_Document_with_404],
#    And a negative response is received.
#
  @S-032
  Scenario: must successfully delete a document by document Id soft delete
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    And a successful call [by another privileged user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
    When a request is prepared with appropriate values,
    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
    Then a positive response is received,
    And the response has all other details as expected.
#    And another call [to Get the Document just deleted] will get the expected response as in [Get_Document_with_404]
#    And a negative response is received
#
#  @S-033 Need to raise a bug with DM team as actual DM store API is not returning 404 code.
#  Scenario: must get an error response for a non-existing document Id
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#    When a request is prepared with appropriate values,
#    And the request [contains a non-existing document Id],
#    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
#    Then a negative response is received,
#    And the response has all the details as expected.

  @S-034
  Scenario: must get an error response for a malformed document Id
    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
    When a request is prepared with appropriate values,
    And the request [contains a malformed document Id],
    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
    Then a negative response is received,
    And the response has all the details as expected.

# This will be covered with service level authorization
#  @S-035 @Ignore
#  Scenario: must get an error response for a document id to which the user doesn't have suffient access of deletion,
#    Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#    And a successful call [by this user to upload a document with mandatory metadata] as in [Default_Document_Upload],
#    And a user with [an active citizen profile in CCD with full permission on the document field but not for the particular document just created],
#    When a request is prepared with appropriate values,
#    And the request [is prepared on behalf of the second user],
#    And the request [contains the id of the document just uploaded above],
#    And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API],
#    Then a negative response is received,
#    And the response has all the details as expected.

  @S-036
  Scenario: generic scenario for Unauthorized

  @S-037
  Scenario: generic scenario for Forbidden

  @S-038
  Scenario: generic scenario for Unsupported Media Type
