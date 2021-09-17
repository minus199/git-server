package com.minus.git.reactive.repo

import com.minus.git.reactive.service.GitRepositoriesService
import mu.KotlinLogging
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

private val logger = KotlinLogging.logger {}

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
    override fun open(client: ServerRequest, name: String): CassandraGitRepository =
        repositories.computeIfAbsent(name) {
            repositoriesService.resolve(name).block()!!
        }

    companion object {
        private val repositories: MutableMap<String, CassandraGitRepository> = HashMap()
    }
}