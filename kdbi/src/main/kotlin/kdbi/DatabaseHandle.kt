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
import kotlin.reflect.KClass

interface DatabaseLock {

    fun release()
}

interface DatabaseHandle : Closeable {

    fun <T : Any> execute(type: KClass<T>, query: String, vararg args: Any?): ResultIterable<T>
    fun execute(query: String, vararg args: Any?): Int
    fun <T : Any> attach(type: KClass<T>): T

    fun lock(id: Long): DatabaseLock

    fun begin()
    fun savepoint()
    fun savepoint(name: String)
    fun rollbackToSavepoint(name: String)
    fun rollback()
    fun commit()
}

inline fun <T> DatabaseLock.use(block: () -> T): T =
    try {
        block()
    } finally {
        release()
    }
