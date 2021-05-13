import ast

"""
This method can be used for case:
'3. Get Correct Ordered Revision History of Versioned Status Of Existing EHR With Two Status Versions (JSON)'
if version order in response won't be mandatory

Using:
1. Connect lib in settings:
Library            ${EXECDIR}/robot/EHR_STATUS_TESTS/C.6_GET_VERSIONED_EHR_STATUS/HelperLib.py
2. Get item from response which contains needed version_id.value:
${item1}    Get Item From List By Version    ${response.body}    ${ehrstatus_uid}
"""
def get_item_from_list_by_version(dict, version):
    items = dict.get('items')
    result = None
    for item in items:
        if (item.get('version_id').get('value') == version):
            result = item
    assert result != None , 'NOT FOUND VERSION: ' + version
    return result