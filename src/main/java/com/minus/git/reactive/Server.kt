package com.minus.git.reactive

import mu.KotlinLogging
import org.eclipse.jgit.transport.Daemon
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import java.io.IOException
import java.net.InetSocketAddress


private val logger = KotlinLogging.logger {}

//@Component
class Server(private val repositoryResolver: RepositoryResolver<*>) {

    private val server: Daemon
    val isRunning: Boolean
        get() = server.isRunning

    init {
        logger.info { "Using ${repositoryResolver::class.simpleName}" }

        server = Daemon(InetSocketAddress(9418)).apply {
            getService("git-receive-pack").isEnabled = true
//            getService("git-upload-pack").isEnabled = true
//            setRepositoryResolver(repositoryResolver)
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
