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

import java.sql.PreparedStatement
import kdbi.DatabaseHandle
import kdbi.Mappers
import kdbi.ResultIterable
import kdbi.annotation.SqlQuery
import kdbi.impl.DatabaseHandleImpl
import kdbi.impl.compiler.Compiler
import kdbi.impl.compiler.QueryType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

internal class FunctionQuery(
    sqlQuery: SqlQuery,
    private val function: KFunction<*>
) {

    @JvmField
    val list: Boolean

    @JvmField
    val set: Boolean

    @JvmField
    val iterator: Boolean

    @JvmField
    val array: Boolean

    @JvmField
    val void: Boolean

    @JvmField
    val returnType: KClass<*>

    @JvmField
    val resultSetType: Int

    @JvmField
    val autoCloseResult: Boolean

    @JvmField
    val autoCloseStatement: Boolean

    @JvmField
    val returnUpdateCount: Boolean

    private val funcParams: List<KParameter> = function.parameters.subList(1, function.parameters.size)
    private val query = Compiler.compileQuery(sqlQuery)
    private val sql: String
    private val indexes = mutableMapOf<String, Int>()

    val type: QueryType
        get() = query.type

    init {
        var s = query.sql
        var offset = 0
        val parameterNames = funcParams.map { it.name }
        query.parameters.forEach { parameter ->
            val start = s.substring(0, parameter.position + offset)
            val end = s.substring(parameter.position + offset + parameter.name.length + 1)
            offset -= parameter.name.length
            s = "$start?$end"
            val index = parameter.name.indexOf('.')
            val parameterName = if (index != -1) {
                parameter.name.substring(0, index).trim()
            } else {
                parameter.name
            }
            val parameterIndex = parameterNames.indexOf(parameterName)
            if (parameterIndex == -1) {
                throw IllegalArgumentException("$function -> $parameterName not found")
            }
            indexes[parameter.name] = parameterIndex
        }
        sql = s
        val rt = function.returnType
        var type = rt.classifier as KClass<*>
        val javaType = type.javaObjectType
        array = javaType.isArray
        list = List::class.java.isAssignableFrom(javaType)
        set = Set::class.java.isAssignableFrom(javaType)
        iterator = !set && !list && (
            Iterable::class.java.isAssignableFrom(javaType) ||
                Iterator::class.java.isAssignableFrom(javaType) ||
                ResultIterable::class.java.isAssignableFrom(javaType)
            )
        if (array || list || set || iterator) {
            type = rt.arguments.first().type!!.classifier as KClass<*>
        }
        returnType = type
        void = returnType == Unit::class

        Mappers.register(type)
        funcParams.forEach {
            it.type.classifier?.let {
                Mappers.register(it as KClass<*>)
            }
        }

        resultSetType = query.resultSetType
        returnUpdateCount = query.returnUpdateCount
        autoCloseResult = query.autoCloseResult
        autoCloseStatement = query.autoCloseStatement
    }

    fun prepare(handle: DatabaseHandle, args: Array<Any?>): PreparedStatement {
        val stmt = (handle as DatabaseHandleImpl).prepareStatement(sql, resultSetType)
        var columnIndex = 1
        query.parameters.forEach { parameter ->
            val index = indexes.getValue(parameter.name)
            val funcParam = funcParams[index]
            val arg = args[index]
            val type = funcParam.type.classifier as KClass<*>
            val mapper = Mappers.findMapper(type)
            when {
                arg == null || mapper != null -> {
                    mapper!!.map(type, emptyList(), stmt, columnIndex++, arg)
                }
                else -> {
                    val parts = parameter.name.split(".")
                    if (parts.size == 1) {
                        Mappers.map(
                            type,
                            funcParam.type.arguments.map { it.type!!.classifier as KClass<*> },
                            stmt,
                            columnIndex++,
                            arg
                        )
                    } else {
                        val map = ObjectHelper[arg::class]
                        val path = parts.subList(1, parts.size).map { it.trim() }
                        val (value, property) = map.getObjectAndProperty(arg, path)
                        if (property != null) {
                            Mappers.map(
                                property.returnType.classifier as KClass<*>,
                                property.returnType.arguments.map { it.type!!.classifier as KClass<*> },
                                stmt,
                                columnIndex++,
                                value
                            )
                        } else {
                            Mappers.map(
                                type,
                                funcParam.type.arguments.map { it.type!!.classifier as KClass<*> },
                                stmt,
                                columnIndex++,
                                value
                            )
                        }
                    }
                }
            }
        }
        return stmt
    }
}
