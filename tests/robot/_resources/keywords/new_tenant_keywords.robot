*** Settings ***
Documentation   There are keywords for a new tenant and user create
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot


*** Keywords ***
prepare tenant and user
   create local session
   create new tenant
   generate password
   create new user
   set credential
   set basic auth
   
   Log    \t HEARTBEAT: ${HEARTBEAT_URL}   console=true
   Log    \t CREDENTIALS: ${CREDENTIALS}    console=true
   Log    \t AUTHORIZATION: ${AUTHORIZATION}    console=true
   Log    \t CREATING_SYSTEM_ID: ${CREATING_SYSTEM_ID}    console=true
   Log    \t CONTROL_MODE: ${CONTROL_MODE}    console=true

set credential
   ${login}=   Catenate   SEPARATOR=@   auto   ${tenant_id}
   @{credentials_new}=   Create List   ${login}   ${password}
   Set global variable   ${CREDENTIALS}   ${credentials_new}
   Set global variable   ${CREATING_SYSTEM_ID}   ${system_name}

set basic auth
   ${basic_decode}=   Catenate   SEPARATOR=:   ${CREDENTIALS}[0]   ${CREDENTIALS}[1]
   ${basic_bytes}=   Encode String To Bytes   ${basic_decode}   UTF-8
   ${basic_encode}=   Evaluate   base64.b64encode($basic_bytes)   modules=base64
   ${basic_string}=   Decode Bytes To String   ${basic_encode}   UTF-8
   ${basic_string}=   Catenate   Basic   ${basic_string}
   ${basic_json}=   Create Dictionary   Authorization   ${basic_string}
   Set global variable   ${AUTHORIZATION}   ${basic_json}

create new tenant
   ${random_index}=   Generate Random String   10   [NUMBERS]
   ${name}=   Catenate   SEPARATOR=   auto   ${random_index}
   Set suite Variable    ${system_name}   ${name}
   ${data}   Create Dictionary   name=${name}   systemName=${name}
   ${resp}=   Post On Session   tenant_session    tenant/create   json=${data}   expected_status=201
   Set suite Variable    ${tenant_id}   ${resp.json()}[id]

create new user
   ${data}   Create Dictionary   login=auto   password=${password}   name=auto   active=true   role=ADMIN   tenantId=${tenant_id}
   ${resp}=   Post On Session   tenant_session    user/create   json=${data}   expected_status=201

create local session
   ${headers}   Create Dictionary   accept=application/json
   create session    tenant_session    ${HEARTBEAT_URL}/api/rest/v1    auth=${ADMIN_CREDENTIALS}   headers=${headers}

generate password
   ${pass_1}=   Generate Random String   1   [UPPER]
   ${pass_2}=   Generate Random String   5   [LOWER]
   ${pass_3}=   Generate Random String   3   [NUMBERS]
   ${password}=   Catenate   SEPARATOR=   ${pass_1}   ${pass_2}   ${pass_3}
   Set suite Variable    ${password}   ${password}
