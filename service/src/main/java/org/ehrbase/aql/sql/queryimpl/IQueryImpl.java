/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.aql.sql.queryimpl;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.jooq.Field;

/**
 * Created by christian on 5/6/2016.
 */
@SuppressWarnings("java:S1452")
public interface IQueryImpl {

    boolean isJsonDataBlock();

    boolean isContainsJqueryPath();

    String getJsonbItemPath();

    enum Clause {SELECT, WHERE, ORDERBY, FROM}

    Field<?> makeField(String templateId, String identifier, I_VariableDefinition variableDefinition, Clause clause);

    Field<?> whereField(String templateId, String identifier, I_VariableDefinition variableDefinition);

    String getItemType();
}
