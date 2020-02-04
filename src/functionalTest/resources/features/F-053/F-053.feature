@F-053
Feature: F-053: Submit case creation as Citizen

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-578
  Scenario: must create and update successfully the respective fields with ACL permissions for a Citizen
    Given a user with [an active Citizen profile in CCD]
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Citizen_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Citizen_Case_Creation_Data]
    And another successful call [to get an update event token for the case just created] as in [S-578-Prerequisite_Citizen_Token_For_Update_Case]
    When a request is prepared with appropriate values
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains updated values for DocumentField2, along with an HTTP-201 Created]
    And the response has all other details as expected
    And another successful call [to get an update event token for the case just created] as in [S-578-Prerequisite_Citizen_Token_For_Update_Case]
    And a call [to update the DocumentField4 of same case by Citizen who doesn't have privilege to update DocumentField4] will get the expected response as in [S-578_Later_Case_Update_By_Citizen]

