/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.queryImpl.translator.testcase.pg10.pgsql;

import org.ehrbase.aql.sql.queryImpl.translator.testcase.UC35;

public class TestUC35 extends UC35 {

    public TestUC35(){
        super();
        this.expectedSqlExpression =
                "select cast(jsonb_extract_path(cast(\"ehr\".\"js_ehr\"(\n" +
                        "  cast(ehr_join.id as uuid), \n" +
                        "  'local'\n" +
                        ") as jsonb),'directory') as jsonb) as \"/directory\" from \"ehr\".\"ehr\" as \"ehr_join\"";
    }
}
