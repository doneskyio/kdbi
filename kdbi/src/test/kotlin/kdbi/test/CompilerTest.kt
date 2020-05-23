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
package kdbi.test

import kdbi.impl.compiler.Compiler
import kotlin.test.Test
import kotlin.test.assertTrue

class CompilerTest {

    @Test
    fun blogAdd() {
        val sql = "insert into blog.blogs (instance, slug, name, description, created) values (:blog.instanceId, :blog.slug, :blog.name, :blog.description, :blog.created) returning id"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun blogAddVersion() {
        val sql = "insert into blog.entry_versions (entry, language, name, description, created, modified, creator, content, assets, state) values (:version.entryId, :version.language, :version.name, :version.description, :version.created, :version.modified, :version.creatorId, (:version.content)::jsonb, :version.assets, 'Draft') returning id"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun blogAddEntryVersions() {
        val sql = "insert into blog.published_entry_versions (blog, entry, slug, version, language, modified) values (:blogId, :entryId, :slug, :versionId, :language, :modified) on conflict on constraint published_entry_versions_pkey do nothing"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun blogDelete() {
        val sql = "delete from blog.blogs where id = :id"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun updateBlog() {
        val sql = "update blog.blogs set name = :blog.name, description = :blog.description where id = :blog.id"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun getCategories() {
        val sql = "select * from blog.categories where blog = :blogId order by name "
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun getEntriesWithOffsetLimit() {
        val sql = "select * from blog.entries where blog = :id and state != 'Deleted' order by created desc offset :offset limit :limit"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun simpleWhere() {
        val sql = "select * from users where id = :id"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun testJoin() {
        val sql = "select * from users u inner join user_roles r on (id = \"user\" and role = :role)"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun insert() {
        val sql = "insert into user_roles (\"user\", role) values (:userId, :role)"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }

    @Test
    fun insertObject() {
        val sql = "insert into users (name) values (:user.name) returning *"
        val query = Compiler.compileQuery(sql)
        assertTrue(query.parameters.isNotEmpty())
    }
}
