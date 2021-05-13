# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Force Tags   refactor



*** Test Cases ***
Main flow create new event COMPOSITION
    [Tags]

    upload OPT    nested/nested.opt

    create EHR    XML

    commit composition (XML)    nested/nested.composition.extdatetimes.xml

    # Check result data
    ${xresp}=           Parse Xml          ${response.text}
    ${xtemplate_id}=    Get Element        ${xresp}      archetype_details/template_id/value
                        Element Text Should Be    ${xtemplate_id}    nested.en.v1

    ${xcomposer}=       Get Element        ${xresp}      composer/name
                        Element Text Should Be    ${xcomposer}    Dr. House

    ${xsetting}=        Get Element    ${xresp}    context/setting/value
                        Element Text Should Be    ${xsetting}    primary nursing care

    [Teardown]    restart SUT
