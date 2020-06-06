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

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kdbi.DatabaseHandle
import kdbi.KdbiQueryException
import kdbi.ResultIterable
import kotlin.reflect.KClass

internal class Handler(
    private val handle: DatabaseHandle,
    private val queries: ClassQueries
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        try {
            val query = queries.getQuery(method)
            val statement = query.prepare(handle, args ?: emptyArray())
            return when {
                query.void -> statement.use { it.execute() }
                query.returnUpdateCount && query.returnType == Int::class -> {
                    statement.use {
                        it.execute()
                        it.updateCount
                    }
                }
                else -> {
                    val factory = ObjectFactory[query.returnType]
                    val result = ResultIterable(
                        statement,
                        statement.executeQuery(),
                        { factory.create(it) },
                        !query.iterator || query.autoCloseResult,
                        !query.iterator || query.autoCloseStatement
                    )
                    when {
                        query.iterator -> result
                        query.array || query.list || query.set ->
                            if (query.set) {
                                result.use { it.toSet() }
                            } else {
                                val list = result.use { it.toList() }
                                if (query.array) {
                                    list.toArray(query.returnType)
                                } else {
                                    list
                                }
                            }
                        else -> result.use { it.firstOrNull() }
                    }
                }
            }
        } catch (e: Throwable) {
            throw KdbiQueryException(e)
        }
    }

    private fun List<*>.toArray(type: KClass<*>): Array<*> {
        val array = java.lang.reflect.Array.newInstance(type.java, 0) as Array<*>
        @Suppress("unchecked_cast", "platform_class_mapped_to_kotlin")
        return (this as java.util.Collection<Any>).toArray(array)
    }
}
