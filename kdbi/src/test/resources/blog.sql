create schema if not exists blog;

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