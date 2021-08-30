package com.minus.git.server

import mu.KotlinLogging
import org.eclipse.jgit.transport.Daemon
import org.eclipse.jgit.transport.DaemonClient
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.InetSocketAddress


private val logger = KotlinLogging.logger {}


class Server(isCassandraEnabled: Boolean) {
    private val repositoryResolver: RepositoryResolver<DaemonClient?>
    private val server: Daemon
    val isRunning: Boolean
        get() = server.isRunning

    init {
        repositoryResolver = if (isCassandraEnabled) {
            CassandraRepositoryResolver()
        } else {
            InMemoryRepositoryResolver()
        }
        logger.info { "Using ${repositoryResolver::class.simpleName}" }

        server = Daemon(InetSocketAddress(9418)).apply {
            getService("git-receive-pack").isEnabled = true
//            getService("git-upload-pack").isEnabled = true
            setRepositoryResolver(repositoryResolver)
        }
    }

    fun start() {
        if (isRunning) return // if already running - no op

        try {
            logger.info { "Server starting..." }
            server.start()
            logger.info { "Server running on port ${server.address}" }
        } catch (e: IOException) {
            println("Failed to start server: " + e.message)
            e.printStackTrace()
        }
    }
}

@Component
class BootServer(@Value("\${gradify.cassandra.isEnabled}") private val isCassandraEnabled: String) :
    ApplicationListener<ApplicationReadyEvent> {

    private val server: Server = Server(isCassandraEnabled == "true")

    @EventListener
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        if (server.isRunning) return // if already running - no op

        server.start()
    }
}