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
package kdbi.impl.jdbc

import javax.sql.DataSource
import org.apache.commons.dbcp2.DriverManagerConnectionFactory
import org.apache.commons.dbcp2.PoolableConnection
import org.apache.commons.dbcp2.PoolableConnectionFactory
import org.apache.commons.dbcp2.PoolingDataSource
import org.apache.commons.pool2.impl.GenericObjectPool

internal object DefaultConnectionConfig {

    val url: String?
        get() = System.getenv("database_url")
    val username: String?
        get() = System.getenv("database_username")
    val password: String?
        get() = System.getenv("database_password")
    val connectionMaxTotal: Int
        get() = System.getenv("database_connection_maxTotal")?.toIntOrNull() ?: 25
    val connectionMaxIdle: Int
        get() = System.getenv("database_connection_maxIdle")?.toIntOrNull() ?: 5
    val connectionMinIdle: Int
        get() = System.getenv("database_connection_minIdle")?.toIntOrNull() ?: 5
}

open class PoolableConnectionFactory(
    private val url: String = DefaultConnectionConfig.url!!,
    private val username: String? = DefaultConnectionConfig.username,
    private val password: String? = DefaultConnectionConfig.password,
    private val connectionMaxTotal: Int = DefaultConnectionConfig.connectionMaxTotal,
    private val connectionMinIdle: Int = DefaultConnectionConfig.connectionMinIdle,
    private val connectionMaxIdle: Int = DefaultConnectionConfig.connectionMaxIdle
) : ConnectionFactory<PoolableConnection>() {

    override fun createDataSource(): DataSource {
        val connectionFactory = PoolableConnectionFactory(
            DriverManagerConnectionFactory(
                url,
                username,
                password
            ),
            null
        ).apply {
            defaultAutoCommit = false
        }
        val objectPool = GenericObjectPool<PoolableConnection>(connectionFactory).apply {
            maxTotal = connectionMaxTotal
            maxIdle = connectionMaxIdle
            minIdle = connectionMinIdle
        }
        connectionFactory.pool = objectPool
        return PoolingDataSource(objectPool)
    }
}
