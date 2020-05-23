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
package kdbi.test.mapper

import java.math.BigDecimal
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Time
import kdbi.Database
import kdbi.Mapper
import kdbi.annotation.ColumnName
import kdbi.annotation.SqlMapper
import kdbi.annotation.SqlQuery
import kdbi.impl.jdbc.DataSourceConnectionFactory
import kdbi.impl.jdbc.DefaultConnectionConfig
import kdbi.impl.jdbc.DriverManagerDataSource
import kdbi.initializeWithReflection
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HelloAttrMapper : Mapper<Any> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Any? =
        HelloAttrId(result.getInt(index))

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Any?) {
        stmt.setInt(index, (value as HelloAttrId).id)
    }
}

@SqlMapper(HelloAttrMapper::class)
interface HelloAttr

data class HelloAttrId(val id: Int) : HelloAttr

data class Hello(
    val name: String,
    @ColumnName("attr_id")
    val attr: HelloAttr
)

class Types(
    val int: Int?,
    val char: Char?,
    val sqlDate: Date?,
    val utilDate: java.util.Date?,
    val array: Array<String>?,
    val short: Short?,
    val float: Float?,
    val byte: Byte?,
    val boolean: Boolean?,
    val time: Time?,
    val numeric: BigDecimal?,
    val double: Double?,
    val long: Long?,
    val list: List<Double>?,
    val set: Set<Float>?,
    val ints: Array<Int>?,
    val longs: Array<Long>?,
    val bools: Array<Boolean>?
)

class TypesNonNull(
    val char: Char,
    val sqlDate: Date,
    val utilDate: java.util.Date,
    val array: Array<String>,
    val short: Short,
    val float: Float,
    val byte: Byte,
    val boolean: Boolean,
    val time: Time,
    val numeric: BigDecimal,
    val double: Double,
    val long: Long,
    val list: List<Double>,
    val set: Set<Float>
)

interface HelloDao {

    @SqlQuery("insert into hello (name, attr_id) values (:hello.name, :hello.attr) returning *")
    fun addHello(hello: Hello): Hello

    @SqlQuery("select * from types")
    fun getTypes(): List<Types>

    @SqlQuery("select * from types")
    fun getTypesNonNull(): List<TypesNonNull>

    @SqlQuery(
        "insert into types (\"int\", \"char\", \"sqlDate\", \"utilDate\", \"array\", " +
            "\"short\", \"float\", \"byte\", " +
            "\"boolean\", \"time\", \"numeric\", \"double\", \"long\", \"list\", \"set\", \"ints\", \"longs\", \"bools\") values (:types.int, :types.char, :types.sqlDate, :types.utilDate, :types.array, :types.short, :types.float, :types.byte, :types.boolean, :types.time, :types.numeric, :types.double, :types.long, :types.list, :types.set, :types.ints, :types.longs, :types.bools)"
    )
    fun addTypes(types: Types)
}

class MapperTest {

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
            it.execute("create table hello (name varchar, attr_id int)")
            it.commit()
        }
    }

    @Test
    fun testMapper() {
        Database.newHandle().use {
            val dao = it.attach(HelloDao::class)
            val hello = dao.addHello(Hello("Hi", HelloAttrId(10)))
            assertEquals("Hi", hello.name)
            assertEquals(10, (hello.attr as HelloAttrId).id)
        }
    }

    @Test
    fun testTypes() {
        Database.newHandle().use {
            it.execute(
                """
                create table types (
                    "int" int,
                    "char" char, 
                    "sqlDate" date, 
                    "utilDate" date, 
                    "array" varchar[], 
                    "short" smallint, 
                    "float" float, 
                    "byte" smallint,
                    "boolean" boolean, 
                    "time" time, 
                    "numeric" numeric(32, 4),
                    "double" double precision,
                    "long" bigint,
                    "list" double precision[],
                    "set" float[],
                    "ints" int[],
                    "longs" bigint[],
                    "bools" boolean[]
                )
                """.trimIndent()
            )
            val dao = it.attach(HelloDao::class)
            dao.addTypes(
                Types(
                    Int.MAX_VALUE,
                    'c',
                    java.sql.Date(System.currentTimeMillis()),
                    java.util.Date(System.currentTimeMillis()),
                    arrayOf("hi", "how", "are", "you"),
                    Short.MAX_VALUE,
                    Float.MAX_VALUE,
                    3,
                    true,
                    Time(System.currentTimeMillis()),
                    BigDecimal.TEN,
                    Double.MAX_VALUE,
                    Long.MAX_VALUE,
                    listOf(1.0, 2.0),
                    setOf(3f, 4f),
                    arrayOf(1, 3, 5),
                    arrayOf(1L, 3L, 5L),
                    arrayOf(true, false)
                )
            )
            dao.addTypes(
                Types(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )
            assertTrue(dao.getTypes().isNotEmpty())
        }
    }
}
