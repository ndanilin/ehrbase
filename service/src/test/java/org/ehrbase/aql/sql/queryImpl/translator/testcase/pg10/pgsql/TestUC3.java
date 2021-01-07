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

import org.ehrbase.aql.sql.queryImpl.translator.testcase.UC3;

public class TestUC3 extends UC3 {

    public TestUC3(){
        super();
        this.expectedSqlExpression =
                "select \"composer_ref\".\"name\" as \"/composer/name\" " +
                "from \"ehr\".\"entry\" " +
                "right outer join \"ehr\".\"composition\" as \"composition_join\" on \"composition_join\".\"id\" = \"ehr\".\"entry\".\"composition_id\" " +
                "join \"ehr\".\"party_identified\" as \"composer_ref\" on \"composition_join\".\"composer\" = \"composer_ref\".\"id\"" +
                " where \"ehr\".\"entry\".\"template_id\" = ?";
    }
}
