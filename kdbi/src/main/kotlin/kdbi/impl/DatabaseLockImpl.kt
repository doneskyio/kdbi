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
package kdbi.impl

import kdbi.DatabaseHandle
import kdbi.DatabaseLock

internal class DatabaseLockImpl(private val id: Long, private val handle: DatabaseHandle) :
    DatabaseLock {

    fun lock() {
        handle.execute("select pg_advisory_lock($id)")
    }

    override fun release() {
        handle.execute("select pg_advisory_unlock($id)")
    }
}
