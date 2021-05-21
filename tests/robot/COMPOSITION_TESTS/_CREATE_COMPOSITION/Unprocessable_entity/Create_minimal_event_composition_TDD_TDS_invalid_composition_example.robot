*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_minimal_event_composition_TDD\TDS_invalid_composition_example
   [Tags]   not-ready   2021
   
   upload OPT   nested/nested.opt
   create EHR
   commit composition (TDD\TDS)    composition=invalid/nested.composition.TDD_TDS.xml
   ...                             template_id=nested.en.v1
   ...                             prefer=minimal
   check status_code of commit composition    422

   [Teardown]    restart SUT

   