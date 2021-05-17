*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_representation_event_composition_FLAT
   upload OPT   nested/nested.opt
   create EHR
   commit composition (FLAT)    composition=valid/nested.composition.FLAT.json
   ...                          template_id=nested.en.v1
   check the successfull result of commit compostion (FLAT)   nesting/_uid

   [Teardown]    restart SUT

