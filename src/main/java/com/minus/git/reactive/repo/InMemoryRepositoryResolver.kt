package com.minus.git.reactive.repo

import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import kotlin.Throws
import java.util.HashMap
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import org.eclipse.jgit.transport.DaemonClient
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import org.eclipse.jgit.lib.Repository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix="gradify.cassandra", name = ["isEnabled"], havingValue = "false", matchIfMissing = true )
internal class InMemoryRepositoryResolver : RepositoryResolver<DaemonClient?> {
    @Throws(
        RepositoryNotFoundException::class,
        ServiceNotAuthorizedException::class,
        ServiceNotEnabledException::class,
        ServiceMayNotContinueException::class
    )
    override fun open(client: DaemonClient?, name: String): Repository {
        var repo = repositories[name]
        if (repo == null) {
            repo = InMemoryRepository(
                DfsRepositoryDescription(name)
            )
            repositories[name] = repo
        }
        return repo
    }

    companion object {
        /**
         * Maps repository names to repository instances
         */
        private val repositories: MutableMap<String, InMemoryRepository> = HashMap()
    }
}