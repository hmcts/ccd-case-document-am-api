@F-460
Feature: F-460: (SPIKE) Create new BEFTA based feature file for invoking document stores download metadata API

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  @S-4601
  Scenario: Create new BEFTA based feature file for invoking document stores download metadata API
    Given a user with [an active Solicitor profile in CCD with a specific variation of ACLs on a case type]
    And a call [Get the document from Document Store] will get the expected response as in [F-460_Test_Data_Base_Get_Dcoument]

