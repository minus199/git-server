package com.minus.git.reactive.repo

import com.minus.git.reactive.repo.store.Tables
import com.minus.git.reactive.service.DatabaseSessionOps
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import java.io.IOException

private val logger = KotlinLogging.logger {}

class CassandraGitRepository(repoDesc: DfsRepositoryDescription?) :
    DfsRepository(object : DfsRepositoryBuilder<DfsRepositoryBuilder<*, *>?, CassandraGitRepository>() {
        @Throws(IOException::class)
        override fun build(): CassandraGitRepository {
            throw UnsupportedOperationException()
        }
    }.setRepositoryDescription(repoDesc)), DatabaseSessionOps {

    private val objdb: DfsObjDatabase
    private val refdb: DfsRefDatabase
    val git: Git by lazy { Git(this) }

    /**
     * Creating a new repository object may result in creating a new repository in the storage layer,
     *      or if the repository identified by "repoDesc" already exists, it will be used instead.
     */
    init {
        ensureSchemas()
        objdb = CassandraObjDatabase(this)
        refdb = CassandraRefDatabase(this)
        logger.info { "Loaded repo ${description.repositoryName}" }
    }

    override val keyspace: String
        get() = "repository_${description.repositoryName}"

    override fun getObjectDatabase(): DfsObjDatabase = objdb

    override fun getRefDatabase(): DfsRefDatabase = refdb

    private fun ensureSchemas() {
        arrayOf(
            "CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};",
            "CREATE TABLE IF NOT EXISTS $keyspace.${Tables.PACK_DESC.tableName} (name varchar PRIMARY KEY, source int, last_modified bigint, size_map map<text, bigint>, object_count bigint, delta_count bigint, extensions int, index_version int);",
            "CREATE TABLE IF NOT EXISTS $keyspace.${Tables.PACK_DATA.tableName} (name varchar PRIMARY KEY, data blob);",
            "CREATE TABLE IF NOT EXISTS $keyspace.${Tables.REFS.tableName} (name varchar PRIMARY KEY, type int, value varchar, aux_value varchar);",
        ).map { it.execute() }
    }
}