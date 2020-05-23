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
import java.util.Calendar
import kdbi.annotation.ColumnName

typealias EntryId = Long
typealias EntrySlug = String

enum class EntryState {
    Draft,
    Published,
    Archived,
    Deleted
}

data class Entry(
    @ColumnName("blog")
    val blogId: BlogId,
    val slug: EntrySlug,
    val name: String,
    val description: String,
    @ColumnName("creator")
    val creatorId: Long,
    val created: Timestamp = Timestamp(System.currentTimeMillis()),
    val published: Timestamp? = null,
    val state: EntryState = EntryState.Draft,
    val id: EntryId = 0
) {

    val year: Int
    val month: Int
    val day: Int

    init {
        if (published == null) {
            year = 0
            month = 0
            day = 0
        } else {
            val c = Calendar.getInstance().apply {
                time = published
            }
            year = c[Calendar.YEAR]
            month = c[Calendar.MONTH] + 1
            day = c[Calendar.DAY_OF_MONTH]
        }
    }
}
