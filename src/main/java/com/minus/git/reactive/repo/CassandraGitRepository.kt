package com.minus.git.reactive.repo

import com.minus.git.reactive.service.DatabaseSessionOps
import com.minus.git.reactive.repo.store.Tables
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import java.io.IOException

class CassandraGitRepository(repoDesc: DfsRepositoryDescription?) :
    DfsRepository(object : DfsRepositoryBuilder<DfsRepositoryBuilder<*, *>?, CassandraGitRepository>() {
        @Throws(IOException::class)
        override fun build(): CassandraGitRepository {
            throw UnsupportedOperationException()
        }
    }.setRepositoryDescription(repoDesc)), DatabaseSessionOps {

    private val objdb: DfsObjDatabase
    private val refdb: DfsRefDatabase

    /**
     * Creating a new repository object may result in creating a new repository in the storage layer,
     *      or if the repository identified by "repoDesc" already exists, it will be used instead.
     */
    init {
        ensureSchemas()
        objdb = CassandraObjDatabase(this)
        refdb = CassandraRefDatabase(this)
    }

    override fun getObjectDatabase(): DfsObjDatabase {
        return objdb
    }

    override fun getRefDatabase(): DfsRefDatabase {
        return refdb
    }

    private fun ensureSchemas() {
        arrayOf(
            "CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};",
            "CREATE TABLE IF NOT EXISTS $keyspace.${Tables.PACK_DESC.dbName} (name varchar PRIMARY KEY, source int, last_modified bigint, size_map map<text, bigint>, object_count bigint, delta_count bigint, extensions int, index_version int);",
            "CREATE TABLE IF NOT EXISTS $keyspace.${Tables.PACK_DATA.dbName} (name varchar PRIMARY KEY, data blob);",
            "CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};",
            "CREATE TABLE IF NOT EXISTS $keyspace.${Tables.REFS.dbName} (name varchar PRIMARY KEY, type int, value varchar, aux_value varchar);",
        ).map { it.execute() }
    }
}