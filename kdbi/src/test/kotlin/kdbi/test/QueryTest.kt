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

import java.sql.ResultSet
import java.sql.Timestamp
import kdbi.Database
import kdbi.ResultIterable
import kdbi.annotation.ColumnName
import kdbi.annotation.SqlQuery
import kdbi.impl.jdbc.DataSourceConnectionFactory
import kdbi.impl.jdbc.DefaultConnectionConfig
import kdbi.impl.jdbc.DriverManagerDataSource
import kdbi.initializeWithReflection
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

enum class Roles {
    Cool,
    Yea
}

data class UserRole(@ColumnName("user") val id: Int, val role: Roles)

data class User(val name: String, val created: Timestamp? = null, val id: Int = 0)

data class User4(val name: String, val roles: List<String>, val id: Int = 0)

class User2 {

    var name: String = ""
    var id: Int = 0
}

data class User3(val name: String?, val id: Int = 0)

interface Users {

    @SqlQuery("select * from users order by name asc", ResultSet.TYPE_SCROLL_INSENSITIVE, false, false, false)
    fun getUsers(): ResultIterable<User>

    @SqlQuery("select count(*) from users")
    fun getUsersCount(): Int

    @SqlQuery("insert into users (name) values (:user.name) returning *")
    fun addUser(user: User): User

    @SqlQuery("insert into users (name, roles) values (:user.name, :user.roles) returning *")
    fun addUser4(user: User4): User4

    @SqlQuery("update users set name = :name", returnUpdateCount = true)
    fun editUsers(name: String): Int

    @SqlQuery("update users set name = :name")
    fun editUserNoResult(name: String)

    @SqlQuery("insert into users (name) values (:user.name) returning *")
    fun addUser3(user: User3): User3

    @SqlQuery("update users set name = :user.name where id = :user.id")
    fun editUser3(user: User3)

    @SqlQuery("select * from users where id = :id")
    fun getUser(id: Int): User

    @SqlQuery("select * from users where id = :id")
    fun getUser3(id: Int): User3

    @SqlQuery("insert into users (name) values (:user.name) returning *")
    fun addUser2(user: User2): User2

    @SqlQuery("insert into user_roles (\"user\", role) values (:userId, :role)")
    fun addUserRole(userId: Int, role: String)

    @SqlQuery("insert into user_roles (\"user\", role) values (:userId, :role)")
    fun addUserRole(userId: Int, role: Roles)

    @SqlQuery("select * from users u inner join user_roles r on (id = \"user\" and role = :role)")
    fun getUsersByRole(role: String): List<User>

    @SqlQuery("select * from users u inner join user_roles r on (id = \"user\" and role = :role)")
    fun getUsersByRoleAsArray(role: String): Array<User>

    @SqlQuery("select * from user_roles")
    fun getRoles(): List<UserRole>

    @SqlQuery("insert into nullable_table (a) values (:n) returning id")
    fun addNullable(n: String?): Int

    @SqlQuery("select a from nullable_table where id = :id")
    fun getNullable(id: Int): String?

    @SqlQuery("select d from times where id = :id")
    fun getTimestamp(id: Int): Timestamp

    @SqlQuery("insert into times (d) values (now())")
    fun addTimestamp()

    @SqlQuery("insert into times (d) values (:t) returning id")
    fun addTimestamp(t: Timestamp): Int

    @SqlQuery("insert into nulls (i, d, t) values (:i, :d, :t)")
    fun addNulls(i: Int?, d: Double?, t: Timestamp?)
}

class QueryTest {

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
    fun addArray() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, roles varchar[])")
            val dao = it.attach(Users::class)
            val user = dao.addUser4(User4("Name", listOf("cool role")))
            assertEquals(listOf("cool role"), user.roles)
        }
    }

    @Test
    fun addNullable() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            it.execute("create table nullable_table (id serial, a varchar)")
            val dao = it.attach(Users::class)
            val id = dao.addNullable(null)
            assertEquals(1, id)

            val a = dao.getNullable(1)
            assertNull(a)

            val newUser = dao.addUser3(User3(null))
            assertNull(newUser.name)
            assertEquals(newUser, dao.getUser3(newUser.id))

            dao.editUser3(newUser.copy(name = "My Name"))

            assertEquals("My Name", dao.getUser3(newUser.id).name)
        }
    }

    @Test
    fun userQuery() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            val dao = it.attach(Users::class)
            val newUser = dao.addUser(User("Kyle"))
            assertEquals(1, newUser.id)
            assertEquals("Kyle", newUser.name)
            assertNotNull(newUser.created)

            val newUserById = dao.getUser(1)
            assertEquals(newUser, newUserById)
        }
    }

    @Test
    fun userCountQuery() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            val dao = it.attach(Users::class)
            dao.addUser(User("Kyle"))
            dao.addUser(User("Kyle 2"))
            dao.addUser(User("Kyle 3"))
            assertEquals(3, dao.getUsersCount())
        }
    }

    @Test
    fun userUpdateQuery() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            val dao = it.attach(Users::class)
            dao.addUser(User("User 1"))
            dao.addUser(User("User 2"))

            val updated = dao.editUsers("New Names for everyone")
            assertEquals(2, updated)
        }
    }

    @Test
    fun userUpdateQuery2() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            val dao = it.attach(Users::class)
            dao.addUser(User("User 1"))
            dao.addUser(User("User 2"))

            it.execute("update users set name = ?", "New Name")

            val users = dao.getUsers().toList()
            assertEquals(2, users.size)
            assertEquals("New Name", users[0].name)
            assertEquals("New Name", users[1].name)
        }
    }

    @Test
    fun userUpdateQuery3() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            val dao = it.attach(Users::class)
            dao.addUser(User("User 1"))
            dao.addUser(User("User 2"))

            dao.editUserNoResult("New Names for everyone")

            assertTrue(dao.getUsers().toList().all { it.name == "New Names for everyone" })
        }
    }

    @Test
    fun userQueryIter() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            val dao = it.attach(Users::class)
            dao.addUser(User("User 1"))
            dao.addUser(User("User 2"))
            dao.addUser(User("User 3"))
            val users = dao.getUsers()
            assertEquals(3, users.toList().size)

            val usersList = users.toList()
            assertEquals("User 1", usersList[0].name)
            assertEquals("User 2", usersList[1].name)
            assertEquals("User 3", usersList[2].name)

            users.close()
        }
    }

    @Test
    fun userQueryJoin() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            it.execute("create table user_roles (\"user\" int, role varchar)")
            val dao = it.attach(Users::class)
            val newUser = dao.addUser(User("Kyle"))
            dao.addUserRole(newUser.id, "hello")

            val users = dao.getUsersByRole("hello")
            assertEquals(newUser, users.first())
        }
    }

    @Test
    fun userQueryJoinArray() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            it.execute("create table user_roles (\"user\" int, role varchar)")
            val dao = it.attach(Users::class)
            val newUser = dao.addUser(User("Kyle"))
            dao.addUserRole(newUser.id, "hello")

            val users = dao.getUsersByRoleAsArray("hello")
            assertEquals(newUser, users.first())
        }
    }

    @Test
    fun userQueryEnum() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp not null default now())")
            it.execute("create table user_roles (\"user\" int, role varchar)")
            val dao = it.attach(Users::class)
            val newUser = dao.addUser(User("Kyle"))
            dao.addUserRole(newUser.id, Roles.Cool)

            val users = dao.getRoles()
            assertEquals(Roles.Cool, users.first().role)
        }
    }

    @Test
    fun user2Query() {
        Database.newHandle().use {
            it.execute("create table users (id serial, name varchar, created timestamp)")
            val user2 = User2()
            user2.name = "Kyle"
            val newUser = it.attach(Users::class)
                .addUser2(user2)
            assertEquals(1, newUser.id)
            assertEquals("Kyle", newUser.name)
        }
    }

    @Test
    fun testTimestamp() {
        Database.newHandle().use {
            it.execute("create table times (id serial, d timestamp)")
            val dao = it.attach(Users::class)
            dao.addTimestamp()
            val d = dao.getTimestamp(1)
            assertNotNull(d)

            val now = System.currentTimeMillis()
            val id = dao.addTimestamp(Timestamp(now))

            assertEquals(now, dao.getTimestamp(id).time)
        }
    }

    @Test
    fun addNulls() {
        Database.newHandle().use {
            it.execute("create table nulls (i int, d double precision, t timestamp)")
            val dao = it.attach(Users::class)
            dao.addNulls(null, null, null)
        }
    }
}
