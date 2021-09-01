package com.minus.git.reactive.backend

import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset

private const val NULL_CHAR = "\u0000"

class ProtocolServiceExecutor internal constructor(val daemon: Daemon, private val socket: Socket) : Closeable {
    val inputStream: BufferedInputStream by lazy { BufferedInputStream(socket.getInputStream()) }
    val outputStream: BufferedOutputStream by lazy { BufferedOutputStream(socket.getOutputStream()) }
    val remotePeer = socket.remoteSocketAddress

    @Throws(IOException::class, ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    internal fun executeService() {
        val cmd = inputStream.bufferedReader(Charset.defaultCharset()).lineSequence().joinToString("\n")
        val extraParameters: List<String> = cmd.extraParams()

        daemon.run { cmd.matchService() }
            ?.execute(this@ProtocolServiceExecutor, cmd.trimHostHeader(), extraParameters)
    }

    private fun String.extraParams() = indexOf(NULL_CHAR.repeat(2)).let { idx ->
        if (idx == -1) emptyList()
        else substring(idx + 2)
            .split(NULL_CHAR.toRegex())
            .dropLastWhile { it.isEmpty() }
    }

    private fun String.trimHostHeader() = indexOf(NULL_CHAR).let {
        // Newer clients hide a "host" header behind this byte.
        // Currently we don't use it for anything, so we ignore this portion of the command.
        if (it == -1) this else substring(0, it)
    }

    override fun close() {
        inputStream.runCatching { close() }
        outputStream.runCatching { close() }
    }
}
