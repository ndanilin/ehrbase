*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_minimal_event_composition_FLAT_ehr_id_does_not_exist
   upload OPT   nested/nested.opt
   create fake EHR
   commit composition (FLAT)    composition=valid/nested.composition.FLAT.json
   ...                          template_id=nested.en.v1
   ...                          prefer=minimal
   ...                          lifecycle=incomplete
   check status_code of commit composition    404

   [Teardown]    restart SUT
