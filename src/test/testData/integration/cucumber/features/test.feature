Feature: Configuration Testing
 
  Scenario: Load configuration file
    Given I have a configuration file
    When I load the configuration
    Then the configuration should be valid 