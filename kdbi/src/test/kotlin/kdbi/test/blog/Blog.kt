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
package kdbi.test.blog

import java.sql.Timestamp
import kdbi.annotation.ColumnName

typealias BlogId = Long
typealias BlogSlug = String

data class Blog(
    @ColumnName("instance")
    val instanceId: Long,
    val slug: BlogSlug,
    val name: String,
    val description: String,
    val created: Timestamp = Timestamp(System.currentTimeMillis()),
    val id: BlogId = 0
)
