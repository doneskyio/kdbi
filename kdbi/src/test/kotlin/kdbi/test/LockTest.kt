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
package kdbi.test

import kdbi.Database
import kdbi.impl.jdbc.DataSourceConnectionFactory
import kdbi.impl.jdbc.DefaultConnectionConfig
import kdbi.impl.jdbc.DriverManagerDataSource
import kdbi.initializeWithReflection
import kdbi.use
import kotlin.test.BeforeTest
import kotlin.test.Test

class LockTest {

    @BeforeTest
    fun initialize() {
        Database.initializeWithReflection(
            DataSourceConnectionFactory(
                DriverManagerDataSource(
                    DefaultConnectionConfig.url ?: "jdbc:postgresql://localhost:5432/kdbi",
                    DefaultConnectionConfig.username ?: "kdbi",
                    DefaultConnectionConfig.password ?: "kdbi"
                )
            )
        )
        Database.newHandle().use {
            it.execute("drop schema if exists public cascade")
            it.execute("create schema public")
        }
    }

    @Test
    fun lockTest() {
        Database.newHandle().use {
            it.lock(1000).use {
                it.execute("select 1")
            }
        }
    }
}
