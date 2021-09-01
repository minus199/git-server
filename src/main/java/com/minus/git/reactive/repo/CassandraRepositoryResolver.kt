package com.minus.git.reactive.repo

import com.minus.git.reactive.backend.ProtocolServiceExecutor
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "gradify.cassandra", name = ["isEnabled"], havingValue = "true")
class CassandraRepositoryResolver : RepositoryResolver<ProtocolServiceExecutor?> {
    @Throws(
        RepositoryNotFoundException::class,
        ServiceNotAuthorizedException::class,
        ServiceNotEnabledException::class,
        ServiceMayNotContinueException::class
    )
    override fun open(client: ProtocolServiceExecutor?, name: String): Repository = repositories.computeIfAbsent(name) {
        try {
            CassandraGitRepository(DfsRepositoryDescription(name.sanitize()))
        } catch (e: Exception) {
            throw ServiceMayNotContinueException(e)
        }
    }

    companion object {
        private val repositories: MutableMap<String, CassandraGitRepository> = HashMap()
    }
}