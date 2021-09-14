package com.minus.git.reactive.service

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Flux

data class RepoName(val name: String)

@Service
class GitRepositoriesService(private val repositoryResolver: RepositoryResolver<ServerRequest>) :
    DatabaseSessionOps {
    fun all(): Flux<RepoName> = "SELECT keyspace_name FROM system_schema.keyspaces;"
        .execute()
        .mapNotNull {
            val repoName = it.getString(0) ?: ""
            if (repoName.startsWith("repository_")) {
                RepoName(repoName)
            } else {
                null
            }
        }
        .let { Flux.fromIterable(it) }

    fun resolve(req: ServerRequest, repoName: String): Repository = repositoryResolver.open(req, repoName)

    /*.run {
        req.queryParam("service")
            .flatMap { it.matchService() }
            .filter { isEnabledFor(it) }
        zzz          .ifPresent{
            it.execute()
        }
        if (isEnabledFor(service)) {
//                    if (extraParameters != null) {
            serviceExecutor.execute(repository, extraParams)
//                    }
        }
    }*/
}