/*
 * A Cassandra backend for JGit
 * Copyright 2014 Ben Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.minus.git.server.repo

import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevWalk
import java.io.IOException

/**
 * A DfsRepository implemented with a Cassandra database store.
 */
class CassandraGitRepository(repoDesc: DfsRepositoryDescription?) :
    DfsRepository(object : DfsRepositoryBuilder<DfsRepositoryBuilder<*, *>?, CassandraGitRepository>() {
        @Throws(IOException::class)
        override fun build(): CassandraGitRepository {
            throw UnsupportedOperationException()
        }
    }.setRepositoryDescription(repoDesc)) {

    private val objdb: DfsObjDatabase
    private val refdb: DfsRefDatabase

    /**
     * Creating a new repository object may result in creating a new repository
     * in the storage layer, or if the repository identified by "repoDesc" already exists, it will be used instead.
     *
     * @param repoDesc  description of the repository that this object will provide access to.
     */
    init {
        objdb = CassandraObjDatabase(this)
        refdb = CassandraRefDatabase(this)
    }

    override fun getObjectDatabase(): DfsObjDatabase {
        return objdb
    }

    override fun getRefDatabase(): DfsRefDatabase {
        return refdb
    }


}