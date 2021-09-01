package com.minus.git.reactive.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

data class RepoName(val name: String)

@Service
class GitRepositoriesService : DatabaseSessionOps {
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
}