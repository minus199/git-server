package com.minus.git.reactive.repo

import mu.KotlinLogging
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.lib.Repository
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
class CassandraRepositoryResolver : RepositoryResolver<ServerRequest> {
    @Throws(
        RepositoryNotFoundException::class,
        ServiceNotAuthorizedException::class,
        ServiceNotEnabledException::class,
        ServiceMayNotContinueException::class
    )
    override fun open(client: ServerRequest, name: String): Repository = repositories.computeIfAbsent(name) {
        try {
            name.sanitize().let {
                logger.info { "Loaded repo $it" }
                CassandraGitRepository(DfsRepositoryDescription(it))
            }

        } catch (e: Exception) {
            throw ServiceMayNotContinueException(e)
        } finally {
            logger.info { "Loaded repo $name" }
        }
    }

    companion object {
        private val repositories: MutableMap<String, CassandraGitRepository> = HashMap()
    }
}