package com.minus.git.reactive.backend

import com.minus.git.reactive.readUpToChar
import java.io.InputStream

private const val NULL_CHAR = "\u0000"

//GET /abcd3/info/refs?service=git-receive-pack HTTP/1.1
//Host: localhost:9045
//User-Agent: git/2.25.1
//Accept: */*
//Accept-Encoding: deflate, gzip, br
//Accept-Language: en-IL, en;q=0.9, *;q=0.8
//Pragma: no-cache
//


class GitHttpRequest(private val inputStream: InputStream) {
    val cmd: String
    val headers: Map<String, String>
    val protocol: String
    val query: Map<String, String>
    val url: String
    val repoName: String
    val verb: String
    val protocolVersion: String


    init {
        val rawRequestLines = inputStream.readUpToChar(' ', 1000).split('\n').map { it.trim() }

        val (_, verb, url, rawQuery, protocol, protocolVersion) = rawRequestLines.matchStatusLine()
        this.verb = verb
        this.url = url
        this.protocol = protocol
        this.protocolVersion = protocolVersion

        this.query = rawQuery.parseQuery()
        this.cmd = this.query["service"]!!
        this.headers = rawRequestLines.parseHeaders()
        this.repoName = url.split('/').filter { it.isNotEmpty() }[0]
    }

    private fun String.parseQuery() = split('&').fold(mutableMapOf<String, String>()) { acc, s ->
        val (k, v) = s.split('=')
        acc.apply { acc[k] = v }
    }.toMap()

    private fun List<String>.matchStatusLine() = statusLine.matchEntire(this[0])!!.groupValues

    private fun List<String>.parseHeaders() = subList(1, size)
        .fold(mutableMapOf<String, String>()) { acc, rawHeader ->
            acc.apply {
                val (_, hName, hValue) = Companion.headerLine.matchEntire(rawHeader)?.groupValues?.toList()
                    ?: Companion.emptyHeader
                if (hName != null && hValue != null) {
                    this[hName] = hValue
                }
            }
        }.toMap()

    private operator fun <T> List<T>.component6(): T = get(5)

    companion object {
        private val statusLine = """^([A-Z]{3,})\s+(.*)?\?(.*)\s([A-Z]{4,})/(.*)""".toRegex()
        private val headerLine = """^([A-Za-z\-]+):\s(.*)$""".toRegex()
        private val emptyHeader = arrayOfNulls<String>(3).toList()
    }
}


//class ProtocolServiceExecutor internal constructor(val daemon: Daemon, private val socket: Socket) : Closeable {
//    val inputStream: BufferedInputStream by lazy { BufferedInputStream(socket.getInputStream()) }
//    val outputStream: BufferedOutputStream by lazy { BufferedOutputStream(socket.getOutputStream()) }
//    val remotePeer = socket.remoteSocketAddress
//
//
//    @Throws(IOException::class, ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
//    internal fun executeService() {
//        val request = GitHttpRequest(inputStream)
//        daemon.run { request.cmd.matchService() }
//            ?.execute(this@ProtocolServiceExecutor, request, null)
//    }
//
//    override fun close() {
//        inputStream.runCatching { close() }
//        outputStream.runCatching { close() }
//    }
//}

