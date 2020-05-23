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

import java.math.BigDecimal
import java.sql.Array as SQLArray
import java.sql.Date as SQLDate
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Time as SQLTime
import java.sql.Timestamp
import java.sql.Types
import java.util.Date as UTILDate
import java.util.concurrent.ConcurrentHashMap
import kdbi.annotation.SqlMapper
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.staticFunctions

object Mappers {

    private val mapperCache = ConcurrentHashMap<KClass<*>, Mapper<*>>()
    private val registered = mutableSetOf<KClass<*>>()

    init {
        mapperCache[String::class] = StringMapper()
        mapperCache[Int::class] = IntMapper()
        mapperCache[Long::class] = LongMapper()
        mapperCache[Double::class] = DoubleMapper()
        mapperCache[Float::class] = FloatMapper()
        mapperCache[Short::class] = ShortMapper()
        mapperCache[BigDecimal::class] = BigDecimalMapper()
        mapperCache[Byte::class] = ByteMapper()
        mapperCache[Char::class] = CharMapper()
        mapperCache[Boolean::class] = BooleanMapper()
        mapperCache[SQLTime::class] = TimeMapper()
        mapperCache[SQLDate::class] = SQLDateMapper()
        mapperCache[UTILDate::class] = UTILDateMapper()
        mapperCache[Timestamp::class] = TimestampMapper()
        mapperCache[Enum::class] = EnumMapper()
        mapperCache[Collection::class] = CollectionMapper()
        mapperCache[SQLArray::class] = ArrayMapper()
    }

    fun register(clazz: KClass<*>) {
        if (registered.contains(clazz)) {
            return
        }
        registered.add(clazz)
        clazz.annotations.filterIsInstance<SqlMapper>()
            .forEach {
                register(clazz, it.value.constructors.first().call())
            }
        clazz.memberProperties.forEach {
            val type = it.returnType.classifier as KClass<*>
            register(type)
        }
    }

    private fun register(clazz: KClass<*>, mapper: Mapper<*>) {
        mapperCache[clazz] = mapper
    }

    internal fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Any? {
        val mapper = findMapper(type) ?: ObjectMapper
        return mapper.map(type, arguments, index, result)
    }

    @Suppress("unchecked_cast")
    internal fun findMapper(type: KClass<*>): Mapper<Any>? {
        val javaType = type.java
        return (
            when {
                Collection::class.java.isAssignableFrom(javaType) -> mapperCache[Collection::class]
                javaType.isArray -> mapperCache[SQLArray::class]
                javaType.isEnum -> mapperCache[Enum::class]
                else -> mapperCache[type]
            }
            ) as? Mapper<Any>?
    }

    internal fun <T> map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: T?
    ) {
        val mapper = findMapper(type) ?: ObjectMapper
        mapper.map(type, arguments, stmt, index, value)
    }
}

private object ObjectMapper : Mapper<Any> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Any? =
        result.getObject(index)

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Any?) =
        if (value == null) {
            stmt.setNull(index, Types.OTHER)
        } else {
            stmt.setObject(index, value)
        }
}

private val KClass<*>.arrayType: String
    get() = when {
        this == Int::class -> "int"
        this == Long::class -> "bigint"
        this == Double::class -> "float8"
        this == Float::class -> "float"
        this == SQLTime::class -> "time"
        this == SQLDate::class || this == UTILDate::class -> "date"
        this == Timestamp::class -> "timestamp"
        this == Short::class || this == Byte::class -> "smallint"
        this == Char::class -> "char"
        this == Boolean::class -> "boolean"
        this == String::class -> "varchar"
        else -> throw UnsupportedOperationException("Unsupported type: $this")
    }

internal class StringMapper : Mapper<String> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): String? =
        result.getString(index)

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: String?) =
        if (value == null) {
            stmt.setNull(index, Types.VARCHAR)
        } else {
            stmt.setString(index, value)
        }
}

internal class EnumMapper : Mapper<Enum<*>> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Enum<*>? {
        val value = result.getString(index)
        return (type.staticFunctions.first { it.name == "values" }.call() as Array<*>)
            .first { it.toString() == value } as Enum<*>
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: Enum<*>?
    ) = stmt.setObject(index, value, Types.OTHER)
}

internal class ArrayMapper : Mapper<Array<*>> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Array<*>? {
        val array = result.getArray(index) ?: return null
        return array.array as Array<*>
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: Array<*>?
    ) {
        val sqlArray = stmt.connection.createArrayOf(arguments.first().arrayType, value)
        stmt.setArray(index, sqlArray)
    }
}

internal class CollectionMapper : Mapper<Collection<*>> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Collection<*>? {
        val array = result.getArray(index)?.array as Array<*>? ?: return null
        if (type == Set::class) {
            return array.toSet()
        }
        return array.toList()
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: Collection<*>?
    ) = if (value == null) {
        stmt.setNull(index, Types.ARRAY)
    } else {
        val arrayType = arguments.first()
        val array = arrayOfNulls<Any>(value.size)
        value.forEachIndexed { i, any ->
            array[i] = any
        }
        val sqlArray = stmt.connection.createArrayOf(arrayType.arrayType, array)
        stmt.setArray(index, sqlArray)
    }
}

internal class IntMapper : Mapper<Int> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Int? {
        val value = result.getInt(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Int?) =
        if (value == null) {
            stmt.setNull(index, Types.INTEGER)
        } else {
            stmt.setInt(index, value)
        }
}

internal class LongMapper : Mapper<Long> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Long? {
        val value = result.getLong(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Long?) =
        if (value == null) {
            stmt.setNull(index, Types.BIGINT)
        } else {
            stmt.setLong(index, value)
        }
}

internal class DoubleMapper : Mapper<Double> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Double? {
        val value = result.getDouble(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Double?) =
        if (value == null) {
            stmt.setNull(index, Types.DOUBLE)
        } else {
            stmt.setDouble(index, value)
        }
}

internal class FloatMapper : Mapper<Float> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Float? {
        val value = result.getFloat(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Float?) =
        if (value == null) {
            stmt.setNull(index, Types.FLOAT)
        } else {
            stmt.setFloat(index, value)
        }
}

internal class ShortMapper : Mapper<Short> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Short? {
        val value = result.getShort(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Short?) =
        if (value == null) {
            stmt.setNull(index, Types.SMALLINT)
        } else {
            stmt.setShort(index, value)
        }
}

internal class BigDecimalMapper : Mapper<BigDecimal> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): BigDecimal? {
        val value = result.getBigDecimal(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: BigDecimal?
    ) = if (value == null) {
        stmt.setNull(index, Types.NUMERIC)
    } else {
        stmt.setBigDecimal(index, value)
    }
}

internal class ByteMapper : Mapper<Byte> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Byte? {
        val value = result.getShort(index)
        if (result.wasNull()) {
            return null
        }
        return value.toByte()
    }

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Byte?) =
        if (value == null) {
            stmt.setNull(index, Types.SMALLINT)
        } else {
            stmt.setByte(index, value)
        }
}

internal class CharMapper : Mapper<Char> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Char? {
        val value = result.getString(index)
        if (result.wasNull()) {
            return null
        }
        return value.first()
    }

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, stmt: PreparedStatement, index: Int, value: Char?) =
        if (value == null) {
            stmt.setNull(index, Types.CHAR)
        } else {
            stmt.setString(index, value.toString())
        }
}

internal class BooleanMapper : Mapper<Boolean> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Boolean? {
        val value = result.getBoolean(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: Boolean?
    ) = if (value == null) {
        stmt.setNull(index, Types.BOOLEAN)
    } else {
        stmt.setBoolean(index, value)
    }
}

internal class TimeMapper : Mapper<SQLTime> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): SQLTime? {
        val value = result.getTime(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: SQLTime?
    ) = if (value == null) {
        stmt.setNull(index, Types.TIME)
    } else {
        stmt.setTime(index, value)
    }
}

internal class SQLDateMapper : Mapper<SQLDate> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): SQLDate? {
        val value = result.getDate(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: SQLDate?
    ) = if (value == null) {
        stmt.setNull(index, Types.TIME)
    } else {
        stmt.setDate(index, value)
    }
}

internal class UTILDateMapper : Mapper<UTILDate> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): UTILDate? {
        val value = result.getTimestamp(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: UTILDate?
    ) = if (value == null) {
        stmt.setNull(index, Types.TIME)
    } else {
        stmt.setTimestamp(index, Timestamp(value.time))
    }
}

internal class TimestampMapper : Mapper<Timestamp> {

    override fun map(type: KClass<*>, arguments: List<KClass<*>>, index: Int, result: ResultSet): Timestamp? {
        val value = result.getTimestamp(index)
        if (result.wasNull()) {
            return null
        }
        return value
    }

    override fun map(
        type: KClass<*>,
        arguments: List<KClass<*>>,
        stmt: PreparedStatement,
        index: Int,
        value: Timestamp?
    ) = if (value == null) {
        stmt.setNull(index, Types.TIMESTAMP)
    } else {
        stmt.setTimestamp(index, value)
    }
}
