/*
 * Copyright 2020 Donesky, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package kdbi.impl.reflect

import java.lang.reflect.Method
import kdbi.annotation.SqlQuery
import kotlin.reflect.KClass
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

internal class ClassQueries(type: KClass<*>) {

    private val queries = mutableMapOf<String, FunctionQuery>()

    init {
        type.functions.forEach { func ->
            func.annotations.filterIsInstance<SqlQuery>().firstOrNull()?.let {
                queries[func.javaMethod.toString()] = FunctionQuery(it, func)
            }
        }
    }

    fun getQuery(method: Method) = queries.getValue(method.toString())
}
