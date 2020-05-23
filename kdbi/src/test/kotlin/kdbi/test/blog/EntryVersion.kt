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
import java.text.SimpleDateFormat
import kdbi.annotation.ColumnName

typealias EntryVersionId = Long

data class Blogger(val id: Long, val name: String)

class EntryVersionDetails(
    val blog: Blog,
    val entry: Entry,
    val version: EntryVersion,
    val categories: List<Category>,
    val tags: List<Tag>,
    val creator: Blogger,
    val canonicalUrl: String
) {

    val published: String by lazy {
        SimpleDateFormat("EEEEE MMMMM dd, yyyy").format(version.created)
    }
}

data class EntryVersion(
    val id: EntryVersionId,
    @ColumnName("entry")
    val entryId: EntryId,
    val language: String,
    val name: String,
    val description: String,
    val created: Timestamp,
    val modified: Timestamp,
    @ColumnName("creator")
    val creatorId: Long,
    val content: String,
    val assets: List<Long> = emptyList(),
    val state: EntryState = EntryState.Draft
)
