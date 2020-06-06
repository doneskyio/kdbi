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
package kdbi.impl.reflect

import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import kdbi.Mappers
import kdbi.annotation.ColumnName
import kdbi.annotation.SqlConstructor
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class ObjectFactory private constructor(private val type: KClass<*>) {

    private open class Type(val name: String, type: KType) {

        val clazz: KClass<*> = type.classifier as KClass<*>
        val arguments: List<KClass<*>> = type.arguments.map { it.type!!.classifier as KClass<*> }
    }

    private class ConstructorArgument(private val parameter: KParameter) : Type(
        parameter.annotations
            .filterIsInstance<ColumnName>()
            .firstOrNull()
            ?.value ?: parameter.name ?: throw IllegalArgumentException(),
        parameter.type
    ) {

        fun set(parameters: MutableMap<KParameter, Any?>, result: ResultSet) {
            val columnIndex = try {
                result.findColumn(name)
            } catch (e: Exception) {
                -1
            }
            if (columnIndex != -1) {
                val value = Mappers.map(clazz, arguments, columnIndex, result)
                parameters[parameter] = value
            }
        }
    }

    private class Property(
        name: String,
        private val property: KMutableProperty1<Any, Any?>
    ) : Type(name, property.returnType) {

        fun set(o: Any, index: Int, result: ResultSet) {
            val value = Mappers.map(clazz, arguments, index, result)
            property.set(o, value)
        }
    }

    private val constructor: KFunction<*> by lazy {
        type.primaryConstructor?.takeIf { it.parameters.isNotEmpty() }
            ?: type.constructors.firstOrNull { it.annotations.any { it is SqlConstructor } }
            ?: type.constructors.first()
    }

    private val constructorParameters = constructor.parameters.map { ConstructorArgument(it) }

    private val memberProperties = type.memberProperties.filterIsInstance<KMutableProperty1<Any, Any?>>().map {
        Property(
            it.annotations
                .filterIsInstance<ColumnName>()
                .firstOrNull()
                ?.value ?: it.name,
            it
        )
    }

    fun create(result: ResultSet): Any? {
        val mapper = Mappers.findMapper(type)
        if (mapper != null) {
            require(result.metaData.columnCount == 1) { "Returned more than one column" }
            return mapper.map(type, emptyList(), 1, result)
        }
        return newInstance(result)
    }

    private fun newInstance(result: ResultSet): Any? {
        val o = if (constructorParameters.isNotEmpty()) {
            val arguments = mutableMapOf<KParameter, Any?>()
            constructorParameters.forEach { parameter ->
                parameter.set(arguments, result)
            }
            constructor.callBy(arguments)
        } else {
            constructor.call()
        } ?: return null
        memberProperties.forEach { property ->
            val columnIndex = try {
                result.findColumn(property.name)
            } catch (e: Exception) {
                -1
            }
            if (columnIndex != -1) {
                property.set(o, columnIndex, result)
            }
        }
        return o
    }

    companion object {

        private val typeFactoryCache = ConcurrentHashMap<KClass<*>, ObjectFactory>()

        operator fun get(clazz: KClass<*>): ObjectFactory {
            var factory = typeFactoryCache[clazz]
            if (factory == null) {
                factory = ObjectFactory(clazz)
                typeFactoryCache[clazz] = factory
            }
            return factory
        }
    }
}
