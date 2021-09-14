package com.minus.git.reactive

import com.minus.git.reactive.backend.Daemon
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

@GitV1Enabled
@Component
class BootServer(private val server: GitServer) : ApplicationListener<ApplicationReadyEvent> {
    @EventListener
    override fun onApplicationEvent(event: ApplicationReadyEvent) = server.start()
}

@GitV1Enabled
@Component
class GitServer(val daemon: Daemon, @Value("\${git.server.port}") private val port: Int) {
    private val processors: ThreadGroup = ThreadGroup("Git-Daemon")

    private val acceptor: Acceptor by lazy {
        Acceptor()
    }

    private val isRunning: Boolean
        get() = acceptor.isRunning

    private val socket: ServerSocket by lazy {
        ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(null as InetAddress?, port), BACKLOG)
        }
    }

    fun start() {
        if (isRunning) return

        logger.info("Starting server on port $port")
        Runtime.getRuntime().addShutdownHook(Thread { stopAndWait() })
        acceptor.start()
    }

    @Throws(InterruptedException::class)
    fun stopAndWait() {
        logger.info("Shutting down server")
        socket.runCatching { close() }
        acceptor.runCatching { shutDown() }
        acceptor.join()
        logger.info("server offline")
    }

    private inner class Acceptor : Thread(processors, "Git-Daemon-Accept") {
        private val running = AtomicBoolean(false)
        val isRunning: Boolean
            get() = running.get()

        override fun start() {
            running.set(true)
            super.start()
        }

        override fun run() {
            setUncaughtExceptionHandler { _, _ -> shutDown() }
            while (isRunning) try {
                logger.info("Waiting on connections....")
                daemon.startClient(socket.accept())
            } catch (e: SocketException) {
                // Test again to see if we should keep accepting.
            } catch (e: IOException) {
                break
            }

            shutDown()
        }

        fun shutDown() {
            running.set(false)

        }
    }

    companion object {
        private const val BACKLOG = 5
    }
}
