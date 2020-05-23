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

import java.io.PrintWriter
import java.lang.UnsupportedOperationException
import java.sql.Connection
import java.sql.ResultSet
import kdbi.Database
import kdbi.ResultIterable
import kdbi.annotation.SqlQuery
import kdbi.impl.jdbc.DefaultConnectionConfig
import kdbi.impl.jdbc.DriverManagerDataSource
import kdbi.impl.jdbc.PoolableConnectionFactory
import kdbi.initializeWithReflection
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

interface ConnUsers {

    @SqlQuery("select * from users order by name asc", ResultSet.TYPE_SCROLL_INSENSITIVE, false, false)
    fun getUsers(): ResultIterable<User>

    @SqlQuery("insert into users (name) values (:user.name) returning *")
    fun addUser(user: User): User
}

class ConnectionTest {

    private val url = DefaultConnectionConfig.url ?: "jdbc:postgresql://localhost:5432/kdbi"

    @BeforeTest
    fun initialize() {
        Database.initializeWithReflection(
            PoolableConnectionFactory(
                url,
                DefaultConnectionConfig.username ?: "kdbi",
                DefaultConnectionConfig.password ?: "kdbi"
            )
        )
        Database.newHandle().use {
            it.execute("drop schema if exists public cascade")
            it.execute("create schema public")
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            it.commit()
        }
    }

    @Test
    fun close() {
        PoolableConnectionFactory(
            url,
            DefaultConnectionConfig.username ?: "kdbi",
            DefaultConnectionConfig.password ?: "kdbi"
        ).close()
    }

    @Test
    fun addUser() {
        Database.newHandle().use {
            it.attach(ConnUsers::class)
                .addUser(User("User Name"))
        }
    }

    @Test
    fun addUserTransaction() {
        Database.newHandle().use {
            it.begin()
            it.attach(ConnUsers::class)
                .addUser(User("User Name"))
            it.commit()
        }
    }

    @Test
    fun addUserTransactionSavepoint() {
        Database.newHandle().use {
            it.begin()
            it.savepoint()
            it.attach(ConnUsers::class)
                .addUser(User("User Name"))
            it.rollback()
            it.commit()
        }
        Database.newHandle().use {
            assertTrue(
                it.attach(ConnUsers::class)
                    .getUsers().toList().isEmpty()
            )
        }
    }

    @Test
    fun addUserTransactionSavepointRollbackName() {
        Database.newHandle().use {
            it.begin()
            it.savepoint("before user 1")
            it.attach(ConnUsers::class)
                .addUser(User("User Name"))
            it.rollbackToSavepoint("before user 1")
            it.commit()
        }
        Database.newHandle().use {
            assertTrue(
                it.attach(ConnUsers::class)
                    .getUsers().toList().isEmpty()
            )
        }
    }

    @Test
    fun testDriverManagerDataSource() {
        val dataSource = DriverManagerDataSource("$url?user=kdbi&password=kdbi")
        dataSource.connection.close()
    }

    @Test
    fun testDriverManagerDataSource2() {
        val dataSource = DriverManagerDataSource(url)
        dataSource.getConnection("kdbi", "kdbi").close()
    }

    @Test
    fun testDriverManagerDataSource3() {
        val dataSource = DriverManagerDataSource(url)
        assertNotNull(dataSource.parentLogger)
        assertNull(dataSource.logWriter)
        dataSource.logWriter = PrintWriter(System.err)
        assertNotNull(dataSource.logWriter)
        assertEquals(0, dataSource.loginTimeout)
        dataSource.loginTimeout = 30
        assertEquals(30, dataSource.loginTimeout)
        assertFalse(dataSource.isWrapperFor(Connection::class.java))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testDriverManagerDataSource4() {
        val dataSource = DriverManagerDataSource(url)
        dataSource.unwrap(Connection::class.java)
    }
}
