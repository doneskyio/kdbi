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
package kdbi.impl

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Savepoint
import kdbi.DatabaseHandle
import kdbi.DatabaseLock
import kdbi.QueryManager
import kdbi.ResultIterable
import kotlin.reflect.KClass

class DatabaseHandleImpl(
    private val connection: Connection,
    private val manager: QueryManager
) : DatabaseHandle {

    private val savepoints = mutableMapOf<String, Savepoint>()

    init {
        if (connection.autoCommit) {
            connection.autoCommit = false
        }
    }

    internal fun prepareStatement(sql: String, resultSetType: Int): PreparedStatement =
        connection.prepareStatement(sql, resultSetType, ResultSet.CONCUR_READ_ONLY)

    override fun <T : Any> execute(type: KClass<T>, query: String, vararg args: Any?): ResultIterable<T> {
//        val stmt = connection.prepareStatement(query)
//        args.forEachIndexed { index, any ->
//            stmt.setObject(index + 1, any)
//        }
//        val factory = ObjectFactory[type]
//        return ResultIterator(
//            stmt,
//            stmt.executeQuery(),
//            {
//                @Suppress("UNCHECKED_CAST")
//                factory.newInstance(it) as T
//            }
//        )
        TODO()
    }

    override fun execute(query: String, vararg args: Any?): Int =
        connection.prepareStatement(query).use {
            args.forEachIndexed { index, any ->
                it.setObject(index + 1, any)
            }
            if (!it.execute()) {
                return it.updateCount
            }
            return 0
        }

    override fun <T : Any> attach(type: KClass<T>): T =
        manager.attach(this, type)

    override fun lock(id: Long): DatabaseLock {
        val lock = DatabaseLockImpl(id, this)
        lock.lock()
        return lock
    }

    override fun begin() {
        connection.createStatement().use {
            it.execute("BEGIN;")
        }
    }

    override fun savepoint() = savepoint("SAVEPOINT_${savepoints.size}")

    override fun savepoint(name: String) {
        savepoints[name] = connection.setSavepoint(name)
    }

    override fun rollbackToSavepoint(name: String) {
        val savepoint = savepoints.getValue(name)
        connection.rollback(savepoint)
    }

    override fun rollback() = connection.rollback()
    override fun commit() = connection.commit()
    override fun close() = connection.close()
}
