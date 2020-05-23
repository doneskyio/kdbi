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

import java.io.ByteArrayInputStream
import kdbi.Database
import kdbi.impl.compiler.Compiler
import kdbi.impl.jdbc.DataSourceConnectionFactory
import kdbi.impl.jdbc.DefaultConnectionConfig
import kdbi.impl.jdbc.DriverManagerDataSource
import kdbi.initializeWithReflection
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlogTest {

    @BeforeTest
    fun initialize() {
        Database.initializeWithReflection(
            DataSourceConnectionFactory(
                DriverManagerDataSource(
                    DefaultConnectionConfig.url ?: "jdbc:postgresql://localhost:5432/kdbi",
                    DefaultConnectionConfig.username ?: "kdbi",
                    DefaultConnectionConfig.password ?: "kdbi"
                )
            )
        )
        Database.newHandle().use {
            ByteArrayInputStream(schema.toByteArray()).use {
                Compiler.compile(it)
            }.forEach { sql ->
                it.execute(sql)
            }
            it.commit()
        }
    }

    @Test
    fun addBlog() {
        Database.newHandle().use {
            val dao = it.attach(BlogDao::class)
            val id = dao.addBlog(Blog(1, "HI", "HI Name", "HI Description"))
            assertEquals(1, id)

            val categoryId = dao.addCategory(
                id,
                "en",
                "slug",
                "Category"
            )
            assertEquals(1, categoryId)

            val entryId = dao.addEntry(Entry(id, "blogslug", "Name", "Desc", 1))
            assertEquals(1, entryId)

            assertTrue(dao.getBlogs().toList().isNotEmpty())
            assertTrue(dao.getEntries(id, 0, 100).isNotEmpty())
            assertTrue(dao.getEntriesAsSet(id, 0, 100).isNotEmpty())
        }
    }

    private val schema =
        """
        drop schema if exists public cascade;
        create schema public;
        drop schema if exists blog cascade;
        create schema blog;

        create table blog.blogs
        (
            id          bigserial    not null,
            instance    int          not null,
            name        varchar(255) not null,
            slug        varchar(255) not null,
            description varchar(500) not null,
            created     timestamp    not null default now(),
            primary key (id),
            unique (instance, slug)
        );

        create type blog.state as enum ('Draft', 'Published', 'Archived', 'Deleted');

        create table blog.categories
        (
            id       bigserial    not null,
            blog     bigint       not null,
            language varchar(10)  not null,
            slug     varchar(255) not null,
            name     varchar(255) not null,
            state    blog.state   not null default 'Draft',
            primary key (id),
            foreign key (blog) references blog.blogs (id),
            unique (blog, language, slug)
        );

        create table blog.tags
        (
            id       bigserial    not null,
            blog     bigint       not null,
            language varchar(10)  not null,
            slug     varchar(255) not null,
            name     varchar(255) not null,
            state    blog.state   not null default 'Draft',
            primary key (id),
            foreign key (blog) references blog.blogs (id),
            unique (blog, language, slug)
        );

        create table blog.entries
        (
            id          bigserial    not null,
            blog        bigint       not null,
            name        varchar(255) not null,
            description varchar(500) not null,
            slug        varchar(255) not null,
            created     timestamp    not null default now(),
            published   timestamp,
            state       blog.state   not null,
            creator     bigint       not null,
            unique (blog, slug),
            foreign key (blog) references blog.blogs (id),
            primary key (id)
        );

        create table blog.entry_versions
        (
            id          bigserial    not null,
            entry       bigint       not null,
            name        varchar(255) not null,
            description varchar(500) not null,
            language    varchar(10)  not null,
            state       blog.state   not null,
            created     timestamp    not null default now(),
            modified    timestamp    not null default now(),
            creator     bigint       not null,
            content     jsonb        not null,
            assets      bigint[]     not null,
            primary key (id),
            foreign key (entry) references blog.entries (id)
        );

        create table blog.entry_version_categories
        (
            version  bigint not null,
            category bigint not null,
            primary key (version, category),
            foreign key (version) references blog.entry_versions (id),
            foreign key (category) references blog.categories (id)
        );

        create table blog.entry_version_tags
        (
            version bigint not null,
            tag     bigint not null,
            primary key (version, tag),
            foreign key (version) references blog.entry_versions (id),
            foreign key (tag) references blog.tags (id)
        );

        create table blog.published_entry_versions
        (
            blog     bigint       not null,
            entry    bigint       not null,
            version  bigint       not null,
            slug     varchar(255) not null,
            language varchar(10)  not null,
            modified timestamp    not null,
            primary key (version, language),
            foreign key (blog) references blog.blogs (id),
            foreign key (entry) references blog.entries (id),
            foreign key (version) references blog.entry_versions (id)
        );

        create index published_entry_versions_ix on blog.published_entry_versions (blog, language);
        """.trimIndent()
}
