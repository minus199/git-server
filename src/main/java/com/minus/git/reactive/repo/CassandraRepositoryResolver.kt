package com.minus.git.reactive.repo

import com.minus.git.reactive.service.GitRepositoriesService
import mu.KotlinLogging
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

private val logger = KotlinLogging.logger {}

@Deprecated(
    "Not needed anymore. Trying to decouple from jgit.transport",
    ReplaceWith("GitRepositoriesService.resolve()", "com.minus.git.reactive.service.GitRepositoriesService")
)
@Component
@ConditionalOnProperty(prefix = "gradify.cassandra", name = ["isEnabled"], havingValue = "true")
class CassandraRepositoryResolver(private val repositoriesService: GitRepositoriesService) :
    RepositoryResolver<ServerRequest> {

    @Throws(
        RepositoryNotFoundException::class,
        ServiceNotAuthorizedException::class,
        ServiceNotEnabledException::class,
        ServiceMayNotContinueException::class
    )
    override fun open(client: ServerRequest?, name: String): CassandraGitRepository =
        repositories.computeIfAbsent(name) {
            name.gitSanitize().blockOptional()
                .filter { repositoriesService.exists(it) }
                .map { CassandraGitRepository(DfsRepositoryDescription(it)) }
                .orElseThrow { RepositoryNotFoundException(it) }
        }

    companion object {
        private val repositories: MutableMap<String, CassandraGitRepository> = HashMap()
    }
}