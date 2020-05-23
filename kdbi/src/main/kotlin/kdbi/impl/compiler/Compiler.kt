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
package kdbi.impl.compiler

import java.io.InputStream
import java.sql.ResultSet
import kdbi.annotation.SqlQuery

object Compiler {

    private fun String.scan(index: Int, find: String): Boolean {
        if (find.length >= length) {
            return false
        }
        for (i in index until find.length) {
            val s = this[i]
            val c = find[i - index]
            if (s != c) {
                return false
            }
        }
        return true
    }

    private val types = QueryType.values().map { it.name.toLowerCase() }

    private fun String.scanForType(): QueryType {
        types.forEach {
            if (scan(0, it)) {
                return when (it) {
                    "select" -> QueryType.SELECT
                    "insert" -> QueryType.INSERT
                    "update" -> QueryType.UPDATE
                    "delete" -> QueryType.DELETE
                    else -> throw IllegalArgumentException()
                }
            }
        }
        throw IllegalArgumentException()
    }

    fun compileQuery(query: SqlQuery): CompiledQuery =
        compileQuery(
            query.value,
            query.resultSetType,
            query.returnUpdateCount,
            query.autoCloseResult,
            query.autoCloseStatement
        )

    fun compileQuery(
        query: String,
        resultSetType: Int = ResultSet.TYPE_FORWARD_ONLY,
        returnUpdateCount: Boolean = true,
        autoCloseResult: Boolean = true,
        autoCloseStatement: Boolean = true
    ): CompiledQuery {
        val type = query.toLowerCase().trim().scanForType()
        val parameters = mutableListOf<Parameter>()
        val parameterName = StringBuilder()
        var start: Int = -1
        var lastChar = 0.toChar()
        for (index in query.indices) {
            val c = query[index]
            when (c) {
                ':' -> {
                    if (start != -1) {
                        throw IllegalStateException()
                    }
                    if (lastChar != ':' && index + 1 != query.length && query[index + 1] != ':') {
                        start = index
                    }
                }
                ' ', ',', ')' -> {
                    if (start != -1) {
                        parameters.add(Parameter(start, parameterName.toString()))
                        parameterName.clear()
                        start = -1
                    }
                }
                else -> {
                    if (start != -1) {
                        parameterName.append(c)
                    }
                }
            }
            lastChar = c
        }
        if (start != -1) {
            parameters.add(Parameter(start, parameterName.toString()))
        }
        return CompiledQuery(
            query,
            type,
            parameters,
            resultSetType,
            returnUpdateCount,
            autoCloseResult,
            autoCloseStatement
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun compile(`in`: InputStream): List<String> =
        `in`.readAllBytes()
            .decodeToString()
            .split(";")
            .map { it.replace('\n', ' ').trim() }
}
