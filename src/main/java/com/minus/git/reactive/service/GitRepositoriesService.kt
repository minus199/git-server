package com.minus.git.reactive.service

import com.minus.git.reactive.repo.CassandraGitRepository
import com.minus.git.reactive.repo.sanitize
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.intellij.lang.annotations.Language
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

data class RepoName(val name: String)

@Service
class GitRepositoriesService() : DatabaseSessionOps {
    override val keyspace: String
        get() = "system_schema"

    @Language("CassandraQL")
    fun all(): Flux<RepoName> = "SELECT keyspace_name FROM ${keyspace}.keyspaces;"
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

    fun create(repoName: String): Mono<out CassandraGitRepository> =
        repoName.sanitize()
            .flatMap { sanName ->
                exists(sanName).filter { !it }.map { sanName }
            }
            .map {
                CassandraGitRepository(DfsRepositoryDescription(it)).apply { create() }
            }
            .switchIfEmpty(Mono.error(RepositoryExistsException(repoName)))

    fun resolve(repoName: String): Mono<out CassandraGitRepository> =
        repoName.sanitize()
            .flatMap { sanName ->
                exists(sanName).filter { it }.map { sanName }
            }
            .map {
                CassandraGitRepository(DfsRepositoryDescription(it))
            }
            .switchIfEmpty(Mono.error(RepositoryNotFoundException(repoName)))

    fun exists(it: String): Mono<Boolean> = //language=CassandraQL
        "SELECT keyspace_name FROM ${keyspace}.keyspaces WHERE keyspace_name='repository_$it'"
            .execute()
            .toMono()
            .map { it.count() == 1 }

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