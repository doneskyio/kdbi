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

import kdbi.impl.DatabaseHandleImpl
import kdbi.impl.jdbc.ConnectionFactory
import kdbi.impl.reflect.QueryManagerImpl
import kotlin.reflect.KClass

interface QueryManager {

    fun <T : Any> attach(handle: DatabaseHandle, type: KClass<T>): T
}

object Database {

    private lateinit var factory: ConnectionFactory<*>
    private lateinit var manager: QueryManager

    fun initialize(factory: ConnectionFactory<*>, manager: QueryManager) {
        Database.factory = factory
        Database.manager = manager
    }

    fun newHandle(): DatabaseHandle = DatabaseHandleImpl(
        factory.getConnection(),
        manager
    )
}

fun Database.initializeWithReflection(factory: ConnectionFactory<*>) {
    initialize(factory, QueryManagerImpl())
}
