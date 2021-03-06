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

import kdbi.annotation.ColumnName

typealias CategoryId = Long
typealias CategorySlug = String

data class Category(
    val slug: CategorySlug,
    val language: String,
    val name: String,
    @ColumnName("blog")
    val blogId: BlogId = 0,
    val id: CategoryId = 0
) {

    override fun toString() = name
}
