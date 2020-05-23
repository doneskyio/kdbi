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

import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.util.logging.Logger
import javax.sql.DataSource

class DriverManagerDataSource(
    val url: String = DefaultConnectionConfig.url!!,
    val username: String? = DefaultConnectionConfig.username,
    val password: String? = DefaultConnectionConfig.password
) : DataSource {

    override fun isWrapperFor(iface: Class<*>?): Boolean = false
    override fun <T : Any?> unwrap(iface: Class<T>?): T = throw UnsupportedOperationException()

    override fun getConnection(): Connection {
        if (username != null && password != null) {
            return DriverManager.getConnection(url, username, password)
        }
        return DriverManager.getConnection(url)
    }

    override fun getConnection(username: String?, password: String?): Connection =
        DriverManager.getConnection(url, username, password)

    override fun getParentLogger(): Logger = Logger.getLogger("kdbi")
    private var logWriter: PrintWriter? = null
    override fun getLogWriter(): PrintWriter? = logWriter
    override fun setLogWriter(out: PrintWriter?) {
        logWriter = out
    }

    private var loginTimeout = 0
    override fun getLoginTimeout(): Int = loginTimeout
    override fun setLoginTimeout(seconds: Int) {
        loginTimeout = seconds
    }
}
