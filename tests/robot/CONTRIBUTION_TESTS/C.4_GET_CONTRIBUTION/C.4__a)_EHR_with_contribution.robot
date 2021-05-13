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
Documentation   Contribution Integration Tests
...
...     Main flow: get existing CONTRIBUTION
...
...     Preconditions:
...         An EHR with known ehr_id exists and has a CONTRIBUTION with known uid.
...
...     Flow:
...         1. Invoke get CONTRIBUTION service by the existing ehr_id and CONTRIBUTION uid
...         2. The result should be positive and retrieve a CONTRIBUTION identified by uid
...
...     Postconditions:
...         None
Metadata        TOP_TEST_SUITE    CONTRIBUTION
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags    refactor



*** Test Cases ***
#https://jira-1989
Main flow: get existing CONTRIBUTION

    [Tags]  our_implementation_false

    upload OPT    minimal/minimal_evaluation.opt

    create EHR

    commit CONTRIBUTION (JSON)    minimal/minimal_evaluation.contribution.json

    retrieve CONTRIBUTION by contribution_uid (JSON)

    check content of retrieved CONTRIBUTION (JSON)

    [Teardown]    restart SUT
