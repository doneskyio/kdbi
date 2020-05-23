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

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses

internal class ObjectHelper private constructor(private val clazz: KClass<*>) {

    private val propertyCache = ConcurrentHashMap<String, KProperty<*>>()

    private fun findProperty(clazz: KClass<*>, name: String): KProperty<*>? =
        clazz.superclasses.let { i ->
            i.forEach { iface -> findProperty(iface, name)?.let { return it } }; null
        } ?: clazz.memberProperties.firstOrNull { it.name == name }

    private fun findProperty(name: String): KProperty<*>? {
        propertyCache[name]?.let { return it }
        findProperty(clazz, name)?.let {
            propertyCache[name] = it
            return it
        }
        return null
    }

    fun getObjectAndProperty(`object`: Any, path: List<String>): Pair<Any?, KProperty<*>?> {
        var result: Any?
        var currentMap = this
        var currentObj: Any? = `object`
        var currentProperty: KProperty<*>? = null
        path.forEach { name ->
            currentProperty = currentMap.findProperty(name) ?: throw IllegalArgumentException("Cannot find path: $path on $`object`")
            result = currentObj?.let {
                try {
                    currentProperty!!.call(currentObj)
                } catch (e: IllegalAccessException) {
                    null
                }
            }
            if (result != null) {
                currentMap = ObjectHelper[currentProperty!!.returnType.classifier as KClass<*>]
                currentObj = result
            } else {
                currentObj = null
            }
        }
        return Pair(currentObj, currentProperty)
    }

    companion object {

        private val beanMapCache = ConcurrentHashMap<KClass<*>, ObjectHelper>()

        operator fun get(clazz: KClass<*>): ObjectHelper {
            var bean = beanMapCache[clazz]
            if (bean == null) {
                bean = ObjectHelper(clazz)
                beanMapCache[clazz] = bean
            }
            return bean
        }
    }
}
