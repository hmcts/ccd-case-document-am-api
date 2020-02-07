@F-037
Feature: F-037: Submit event for an existing case (V2)

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-577
  Scenario: must update successfully all and only the respective fields with update access for a Case Worker and a Solicitor
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    And a user with [an active solicitor profile with another specific variation of ACLs on the same case type]
    And a user with [an active Solicitor profile having full permissions on the same case type]
    And a successful call [to create a token for case creation] as in [Befta_Jurisdiction2_Default_Token_Creation_Data_For_Case_Creation]
    And another successful call [by a privileged user with full ACL to create a case of this case type] as in [Befta_Jurisdiction2_Default_Full_Case_Creation_Data]
    And another successful call [to get an update event token for the case just created] as in [S-577-Prerequisite_Solicitor_2_Token_For_Update_Case]
    When a request is prepared with appropriate values
    And the request [prompts an update to DocumentField2, made by the privileged user who just created the case]
    And it is submitted to call the [submit event for an existing case (V2)] operation of [CCD Data Store]
    Then a positive response is received
    And the response [contains updated values for DocumentField2]
    And the response has all other details as expected
    And a successful call [to get an update event token for the case just created] as in [S-577-Prerequisite_Solicitor_1_Token_For_Update_Case]
    And a call [to update the same case by Solicitor 1, who doesn't have UPDATE permission] will get the expected response as in [S-577_Later_Case_Update_By_Solicitor_1]
    And a call [to get the same case by Solicitor 1] will get the expected response as in [S-577_Later_Case_Read_By_Solicitor_1]
    And another successful call [to get an update event token for the case just created] as in [S-577-Prerequisite_Solicitor_3_Token_For_Update_Case]
    And a call [to update the same case by Solicitor 3] will get the expected response as in [S-577_Later_Case_Update_By_Solicitor_3]
    And a call [to get the same case by Solicitor 3, who doesn't have READ permission] will get the expected response as in [S-577_Later_Case_Read_By_Solicitor_3]

