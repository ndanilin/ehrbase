*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     Precondition
Suite Teardown  restart SUT


*** Test Cases ***
Main flow create new event COMPOSITION RAW_JSON
    commit composition   format=RAW_JSON
    ...                  composition=nested.en.v1__full.json
    check the successfull result of commit compostion

Main flow create new event COMPOSITION RAW_XML
    commit composition   format=RAW_XML
    ...                  composition=nested.en.v1__full.xml
    check the successfull result of commit compostion

Main flow create new event COMPOSITION FLAT
    commit composition   format=FLAT
    ...                  composition=nested.en.v1__full.json
    check the successfull result of commit compostion   nesting

Main flow create new event COMPOSITION TDD
    commit composition   format=TDD
    ...                  composition=nested.en.v1__full.xml
    check the successfull result of commit compostion

Main flow create new event COMPOSITION STRUCTURED
    commit composition   format=STRUCTURED
    ...                  composition=nested.en.v1__full.json
    check the successfull result of commit compostion   nesting

*** Keywords ***
Precondition
    upload OPT    nested/nested.opt
    create EHR