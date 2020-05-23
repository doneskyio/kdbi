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
import kdbi.annotation.SqlQuery

internal interface BlogDao {

    @SqlQuery("select * from blog.blogs where instance = :id")
    fun getBlogs(id: Long): List<Blog>

    @SqlQuery("select * from blog.blogs where id = :id")
    fun getBlog(id: BlogId): Blog

    @SqlQuery("select * from blog.blogs where slug = :slug")
    fun getBlog(slug: BlogSlug): Blog?

    @SqlQuery("insert into blog.blogs (instance, slug, name, description, created) values (:blog.instanceId, :blog.slug, :blog.name, :blog.description, :blog.created) returning id")
    fun addBlog(blog: Blog): BlogId

    @SqlQuery("update blog.blogs set name = :blog.name, description = :blog.description where id = :blog.id")
    fun editBlog(blog: Blog)

    @SqlQuery("delete from blog.blogs where id = :id")
    fun deleteBlog(id: BlogId)

    @SqlQuery("insert into blog.entries (blog, name, description, slug, created, state, creator) values (:entry.blogId, :entry.name, :entry.description, :entry.slug, :entry.created, :entry.state, :entry.creatorId) returning id")
    fun addEntry(entry: Entry): EntryId

    @SqlQuery("update blog.entries set name = :entry.name, description = :entry.description, slug = :entry.slug, state = :entry.state where id = :entry.id")
    fun editEntry(entry: Entry)

    @SqlQuery("select * from blog.entries where id = :id")
    fun getEntry(id: EntryId): Entry

    @SqlQuery("select * from blog.entries where slug = :slug")
    fun getEntry(slug: EntrySlug): Entry?

    @SqlQuery("select * from blog.entries where blog = :id and state != 'Deleted' order by created desc offset :offset limit :limit")
    fun getEntries(id: BlogId, offset: Long, limit: Int): List<Entry>

    @SqlQuery("select * from blog.entries where blog = :id and state != 'Deleted' order by created desc offset :offset limit :limit")
    fun getEntriesAsSet(id: BlogId, offset: Long, limit: Int): Set<Entry>

    @SqlQuery("update blog.entries set state = :state, published = :published where id = :id")
    fun setEntryState(id: EntryId, state: EntryState, published: Timestamp?)

    @SqlQuery("select distinct language from blog.entry_versions where entry = :id and state = 'Published'")
    fun getLanguages(id: EntryId): List<String>

    @SqlQuery("delete from blog.entries where id = :id")
    fun deleteEntry(id: EntryId)

    @SqlQuery("insert into blog.entry_versions (entry, language, name, description, created, modified, creator, content, assets, state) values (:version.entryId, :version.language, :version.name, :version.description, :version.created, :version.modified, :version.creatorId, (:version.content)::jsonb, :version.assets, 'Draft') returning id")
    fun addEntryVersion(version: EntryVersion): EntryVersionId

    @SqlQuery("insert into blog.entry_version_tags (version, tag) values (:versionId, :tagId)")
    fun addEntryVersionTag(versionId: EntryVersionId, tagId: TagId)

    @SqlQuery("insert into blog.entry_version_categories (version, category) values (:versionId, :categoryId)")
    fun addEntryVersionCategory(versionId: EntryVersionId, categoryId: CategoryId)

    @SqlQuery("delete from blog.entry_version_tags where version = :versionId")
    fun deleteEntryVersionTags(versionId: EntryVersionId)

    @SqlQuery("delete from blog.entry_version_categories where version = :versionId")
    fun deleteEntryVersionCategories(versionId: EntryVersionId)

    @SqlQuery("update blog.entry_versions set name = :version.name, language = :version.language, modified = :version.modified, content = (:version.content)::jsonb, assets = :version.assets where id = :version.id")
    fun editEntryVersion(version: EntryVersion)

    @SqlQuery("select * from blog.entry_versions where id = :id")
    fun getEntryVersion(id: EntryVersionId): EntryVersion

    @SqlQuery("select t.slug from blog.entry_version_tags evt inner join blog.tags t on (t.id = evt.tag and evt.version = :id)")
    fun getEntryVersionTagSlugs(id: EntryVersionId): List<TagSlug>

    @SqlQuery("select t.id from blog.entry_version_tags evt inner join blog.tags t on (t.id = evt.tag and evt.version = :id)")
    fun getEntryVersionTagIds(id: EntryVersionId): List<TagId>

    @SqlQuery("select c.slug from blog.entry_version_categories evc inner join blog.categories c on (c.id = evc.category and evc.version = :id)")
    fun getEntryVersionCategorySlugs(id: EntryVersionId): List<CategorySlug>

    @SqlQuery("select c.id from blog.entry_version_categories evc inner join blog.categories c on (c.id = evc.category and evc.version = :id)")
    fun getEntryVersionCategoryIds(id: EntryVersionId): List<CategoryId>

    @SqlQuery("select * from blog.entry_versions where entry = :id and state != 'Deleted' order by created desc")
    fun getEntryVersions(id: EntryId): List<EntryVersion>

    @SqlQuery("select * from blog.entry_versions where entry = :id and language = :language and state != 'Deleted'")
    fun getEntryVersions(id: EntryId, language: String): List<EntryVersion>

    @SqlQuery("select * from blog.entry_versions where entry = :id and language = :language and state = 'Published'")
    fun getPublishedEntryVersion(id: EntryId, language: String): EntryVersion?

    @SqlQuery("select slug from blog.published_entry_versions where blog = :id and language = :language order by modified desc limit 25")
    fun getPublishedEntrySlugs(id: BlogId, language: String): List<EntrySlug>

    @SqlQuery("select pev.slug from blog.published_entry_versions pev inner join blog.entry_version_tags evt on (evt.version = pev.version and evt.tag = :tagId) where pev.blog = :id and pev.language = :language order by pev.modified desc limit 25")
    fun getPublishedEntrySlugsByTag(id: BlogId, tagId: TagId, language: String): List<EntrySlug>

    @SqlQuery("select pev.slug from blog.published_entry_versions pev inner join blog.entry_version_categories evc on (evc.version = pev.version and evc.category = :categoryId) where pev.blog = :id and pev.language = :language order by pev.modified desc limit 25")
    fun getPublishedEntrySlugsByCategory(id: BlogId, categoryId: CategoryId, language: String): List<EntrySlug>

    @SqlQuery("insert into blog.published_entry_versions (blog, entry, slug, version, language, modified) values (:blogId, :entryId, :slug, :versionId, :language, :modified) on conflict on constraint published_entry_versions_pkey do nothing")
    fun addPublishedEntryVersion(
        blogId: BlogId,
        entryId: EntryId,
        slug: EntrySlug,
        versionId: EntryVersionId,
        language: String,
        modified: Timestamp
    )

    @SqlQuery("delete from blog.published_entry_versions where version = :versionId")
    fun deletePublishedEntryVersion(versionId: EntryVersionId)

    @SqlQuery("update blog.entry_versions set state = :state where id = :id")
    fun setEntryVersionState(id: EntryVersionId, state: EntryState)

    @SqlQuery("delete from blog.entry_versions where id = :id")
    fun deleteEntryVersion(id: EntryVersionId)

    @SqlQuery("insert into blog.categories (blog, language, slug, name) values (:blogId, :language, :slug, :name) returning id")
    fun addCategory(blogId: BlogId, language: String, slug: String, name: String): CategoryId

    @SqlQuery("select * from blog.categories where blog = :blogId and language = :language and state = 'Published' order by name")
    fun getCategories(blogId: BlogId, language: String): List<Category>

    @SqlQuery("select * from blog.categories where blog = :blogId order by name")
    fun getCategories(blogId: BlogId): List<Category>

    @SqlQuery("select * from blog.categories where id = :id")
    fun getCategory(id: CategoryId): Category

    @SqlQuery("select * from blog.categories where slug = :slug")
    fun getCategory(slug: CategorySlug): Category

    @SqlQuery("select * from blog.categories where blog = :blogId and name = :name and language = :language")
    fun getCategory(blogId: BlogId, name: String, language: String): Category?

    @SqlQuery("update blog.categories set state = 'Published' where id = :id and state != 'Published'")
    fun publishCategory(id: CategoryId)

    @SqlQuery("insert into blog.tags (blog, language, slug, name) values (:blogId, :language, :slug, :name) returning id")
    fun addTag(blogId: BlogId, language: String, slug: String, name: String): TagId

    @SqlQuery("select * from blog.tags where blog = :blogId and language = :language and state = 'Published' order by name")
    fun getTags(blogId: BlogId, language: String): List<Tag>

    @SqlQuery("select * from blog.tags where blog = :blogId order by name")
    fun getTags(blogId: BlogId): List<Tag>

    @SqlQuery("update blog.tags set state = 'Published' where id = :id and state != 'Published'")
    fun publishTag(id: TagId)

    @SqlQuery("select * from blog.tags where id = :id")
    fun getTag(id: TagId): Tag

    @SqlQuery("select * from blog.tags where slug = :slug")
    fun getTag(slug: TagSlug): Tag

    @SqlQuery("select * from blog.tags where blog = :blogId and name = :name and language = :language")
    fun getTag(blogId: BlogId, name: String, language: String): Tag?

    @SqlQuery("select * from blog.blogs")
    fun getBlogs(): Iterable<Blog>
}
