#@F-008
#Feature: F-008: Implementation of Service Authorisation
#
#    Background: Load test data for the scenario
#    Given an appropriate test context as detailed in the test data source
#
#    #S-101 to S-104 to be moved to F-007
#
#    @S-101
#    Scenario: must successfully access only Attach Documents to Case API from CCD Data Store
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
#      And Attach Documents to Case service call from CCD Data Store having necessary data in Static Config,
#      When a request is prepared with appropriate values,
#      And the request [contains valid Service authorization token for CCD Data Store Service],
#      And the request [is to be made on behalf of the same service which has necessary data in Static Config],
#      And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
#      Then a positive response is received,
#      And the response has all other details as expected,
#      And a call [to get the metadata of same document just attached to the case] will get the expected response as in [Get_Document_Metadata].
#
#    @S-102
#    Scenario: must receive an error response when Ex-UI tries to access Attach Document to a Case API
#    Given a service with [an active profile in Service Authorization App with limited permissions on Attach Documents To Case],
#    And a user with [an active caseworker profile in CCD with full permissions on a document field],
#    And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
#    When a request is prepared with appropriate values,
#    And the request [is to be made on behalf of the same service which has valid Service authorization token],
#    And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
#    Then a negative response is received,
#    And the response has all the details as expected.
#
#    @S-103
#    Scenario: must receive an error response when Bulk Scan tries to access Attach Document to a Case API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Attach Documents To Case],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-104
#    Scenario: must receive an error response when API-Gateway tries to access Attach Document to a Case API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Attach Documents To Case],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [F-007-Upload_Document1],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Attach Documents To Case] operation of [CCD Case Document AM API],
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    #S-105 to S-108 to be moved to F-006
#    @S-105
#    Scenario: must successfully access Get hashtoken API from Bulk Scan
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
#      When a request is prepared with appropriate values,
#      And the request [contains valid Service authorization token for Bulk Scan Service],
#      And the request [is to be made on behalf of the same service which has necessary data in Static Config],
#      And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API]
#      Then a positive response is received,
#      And the response has all other details as expected.
#
#    @S-106
#    Scenario: must receive an error response when Ex-UI tries to access Get hashtoken API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get hashtoken],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-107
#    Scenario: must receive an error response when CCD Data Store tries to access Get hashtoken API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get hashtoken],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-108
#    Scenario: must receive an error response when API-Gateway tries to access Get hashtoken API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get hashtoken],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get hashtoken by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    #S-109 to S-112 to be moved to F-001
#    @S-109
#    Scenario: must successfully access Get Document Metadata API from Ex-UI
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [contains valid Service authorization token for Ex-UI Service],
#      And the request [is to be made on behalf of the same service which has necessary data in Static Config],
#      And it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
#      Then a positive response is received
#      And the response [contains the metadata for the document uploaded above]
#      And the response has all other details as expected
#
#    @S-110
#    Scenario: must receive an error response when CCD Data Store tries to access Get Document Metadata API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get Document Metadata],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-111
#    Scenario: must receive an error response when Bulk Scan tries to access Get Document Metadata API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get Document Metadata],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-112
#    Scenario: must receive an error response when API-Gateway tries to access Get Document Metadata API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get Document Metadata],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get Document Metadata by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    #S-113 to S-116 to be moved to F-002
#    @S-113
#    Scenario: must successfully access Get Document Binary Content API from Ex-UI
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [contains valid Service authorization token for Ex-UI Service],
#      And the request [is to be made on behalf of the same service which has necessary data in Static Config],
#      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
#      Then a positive response is received,
#      And the response [contains the binary content for the uploaded document],
#      And the response has all other details as expected.
#
#    @S-114
#    Scenario: must receive an error response when CCD Data Store tries to access Get Document Binary Content API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get Document Binary Content],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-115
#    Scenario: must receive an error response when Bulk Scan tries to access Get Document Binary Content API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get Document Binary Content],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-116
#    Scenario: must receive an error response when API-Gateway tries to access Get Document Binary Content API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Get Document Binary Content],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Get Binary Content by Document ID] operation of [CCD Case Document AM API],
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    #S-117 to S-120 to be moved to F-004
#    @S-117
#    Scenario: must successfully access Upload Document API from Ex-UI
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      When a request is prepared with appropriate values,
#      And the request [contains valid Service authorization token for Ex-UI Service],
#      And the request [contains valid Jurisdiction Id and Case Type Id for Ex-UI Service],
#      And the request [is to be made on behalf of the same service which has necessary data in Static Config],
#      And it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API]
#      Then a positive response is received
#      And the response [contains the metadata for the document uploaded above]
#      And the response has all other details as expected
#
#    @S-118
#    Scenario: must receive an error response when CCD Data Store tries to access Upload Document API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Upload Document],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And the request [contains valid Jurisdiction Id and Case Type Id],
#      And it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-119
#    Scenario: must receive an error response when Bulk Scan tries to access Upload Document API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Upload Document],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And the request [contains valid Jurisdiction Id and Case Type Id],
#      And it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-120
#    Scenario: must receive an error response when API-Gateway tries to access Upload Document API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Upload Document],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And the request [contains valid Jurisdiction Id and Case Type Id],
#      And it is submitted to call the [Upload Document With Binary Content] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    #S-121 to S-124 to be moved to F-005
#    @S-121
#    Scenario: must successfully access Patch Document with ttl API from Ex-UI
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [contains document id uploaded above with ttl]
#      And the request [contains valid Service authorization token for Ex-UI Service],
#      And the request [is to be made on behalf of the same service which has necessary data in Static Config],
#      And it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
#      Then a positive response is received
#      And the response [contains the same ttl and document ID uploaded above]
#      And the response has all other details as expected
#
#    @S-122
#    Scenario: must receive an error response when CCD Data Store tries to access Patch Document with ttl API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Patch Document with ttl],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-123
#    Scenario: must receive an error response when Bulk Scan tries to access Patch Document with ttl API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Patch Document with ttl],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-124
#    Scenario: must receive an error response when API-Gateway tries to access Patch Document with ttl API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Patch Document with ttl],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Patch Document with ttl] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#
#    #S-125 to S-128 to be moved to F-003
#    @S-125
#    Scenario: must successfully access Delete Document API from Ex-UI
#      Given a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [contains valid Service authorization token for Ex-UI Service],
#      And the request [is to be made on behalf of the same service which has necessary data in Static Config],
#      And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API]
#      Then a positive response is received
#      And the response has all other details as expected
#      And another call [to Get the Document just deleted] will get the expected response as in [Get_Document_with_404]
#      And a negative response is received
#
#    @S-126
#    Scenario: must receive an error response when CCD Data Store tries to access Delete Document API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Delete Document],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-127
#    Scenario: must receive an error response when Bulk Scan tries to access Delete Document API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Delete Document],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
#    @S-128
#    Scenario: must receive an error response when API-Gateway tries to access Delete Document API
#      Given a service with [an active profile in Service Authorization App with limited permissions on Delete Document],
#      And a user with [an active caseworker profile in CCD with full permissions on a document field],
#      And a successful call [by same user to upload a document with mandatory metadata] as in [Default_Document_Upload_Data],
#      When a request is prepared with appropriate values,
#      And the request [is to be made on behalf of the same service which has valid Service authorization token],
#      And it is submitted to call the [Delete Document by Document ID] operation of [CCD Case Document AM API]
#      Then a negative response is received,
#      And the response has all the details as expected.
#
