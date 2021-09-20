package com.minus.git.reactive.service

import com.minus.git.reactive.repo.CassandraGitRepository
import com.minus.git.reactive.repo.createDefaultBranch
import com.minus.git.reactive.repo.gitSanitize
import mu.KotlinLogging
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.intellij.lang.annotations.Language
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

data class RepoName(val name: String)

private val logger = KotlinLogging.logger {}

@Service
class GitRepositoriesService() :
    DatabaseSessionOps {

    override val keyspace: String
        get() = "system_schema"

    @Language("CassandraQL")
    fun all(): Flux<RepoName> = "SELECT keyspace_name FROM ${keyspace}.keyspaces;" //todo: create repos table
        .execute()
        .let { Flux.fromIterable(it) }
        .mapNotNull {
            val repoName = it.getString(0) ?: ""
            if (repoName.startsWith("repository_")) {
                RepoName(repoName)
            } else {
                null
            }
        }

    fun create(repoName: String): Mono<CassandraGitRepository> = resolve(repoName, false)
        .doOnNext { it.createDefaultBranch() }

    fun resolve(repoName: String, validateExists: Boolean = true): Mono<CassandraGitRepository> =
        repoName.gitSanitize().map { repoNameSanitized ->
            repositories.computeIfAbsent(repoName) {
                if (!validateExists || exists(repoNameSanitized)) {
                    CassandraGitRepository(DfsRepositoryDescription(it))
                } else {
                    throw RepositoryNotFoundException(repoNameSanitized)
                }.apply {
                    logger.info { "Loaded repo ${this.description.repositoryName}" }
                }
            }
        }

    fun exists(it: String): Boolean = //language=CassandraQL
        "SELECT keyspace_name FROM ${keyspace}.keyspaces WHERE keyspace_name='repository_$it'"
            .execute().count() == 1

    companion object {
        private val repositories: MutableMap<String, CassandraGitRepository> = ConcurrentHashMap()
    }
    /*.run {
        req.queryParam("service")
            .flatMap { it.matchService() }
            .filter { isEnabledFor(it) }
            .ifPresent{
            it.execute()
        }
        if (isEnabledFor(service)) {
//                    if (extraParameters != null) {
            serviceExecutor.execute(repository, extraParams)
//                    }
        }
    }*/
}