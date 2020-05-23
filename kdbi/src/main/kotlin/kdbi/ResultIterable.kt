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
package kdbi

import java.io.Closeable
import java.lang.Exception
import java.sql.ResultSet
import java.sql.Statement

class ResultIterable<T>(
    private val statement: Statement,
    private val result: ResultSet,
    private val mapper: (result: ResultSet) -> T,
    private val autoCloseResult: Boolean,
    private val autoCloseStatement: Boolean
) : Iterable<T>, Iterator<T>, AutoCloseable, Closeable {

    private var closed = false
    private var used = false

    override fun iterator(): Iterator<T> {
        if (used && !result.isBeforeFirst) {
            result.beforeFirst()
        }
        return this
    }

    override fun hasNext(): Boolean {
        used = true
        val next = result.next()
        if (!next) {
            close()
        }
        return next
    }

    override fun next(): T = mapper(result)

    fun close(closeResult: Boolean, closeStatement: Boolean) {
        closed = true
        if (closeResult) {
            try {
                result.close()
            } catch (e: Exception) {
            }
        }
        if (closeStatement) {
            try {
                statement.close()
            } catch (e: Exception) {
            }
        }
    }

    override fun close() = close(autoCloseResult, autoCloseStatement)
}
