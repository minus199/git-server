package com.minus.git.reactive.http.api

import com.minus.git.reactive.backend.GitConst
import com.minus.git.reactive.backend.ProtocolService
import com.minus.git.reactive.backend.SupportedServices
import com.minus.git.reactive.component1
import com.minus.git.reactive.component2
import com.minus.git.reactive.service.GitRepositoriesService
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream

fun commonHeaders(contentType: MediaType = MediaType.APPLICATION_JSON): ServerResponse.BodyBuilder = ServerResponse.ok()
    .contentType(contentType)
    .headers {
        it.pragma = "no-cache"
        it.cacheControl = "no-cache, max-age=0, must-revalidate"
        it.connection = listOf("keep-alive")
    }

interface ServerRequestOpx {
    fun ServerRequest.requestedGitService(): GitConst.Cmd =
        queryParam("service")
            .map { it.uppercase().replace('-', '_') }
            .map { GitConst.Cmd.valueOf(it) }
            .orElseThrow()

    fun ServerRequest.reduceBody(): Mono<DataBuffer> = exchange().request.body
        .reduce(DefaultDataBufferFactory.sharedInstance.allocateBuffer()) { acc, db -> acc.write(db) }
}

interface RepoApiRequestOpx : ServerRequestOpx {
    val repositoriesService: GitRepositoriesService

    fun ServerRequest.searchRepo() = repositoriesService
        .resolve(pathVariable("repoName"))
}

interface RepoRequestOpx : RepoApiRequestOpx {
    val services: SupportedServices

    fun ServerRequest.service(): Mono<ProtocolService> = services.match(requestedGitService())

    fun GitConst.Cmd.matchService(): Mono<ProtocolService> = services.match(this)

    fun ServerRequest.executeService(service: ProtocolService): Mono<ServerResponse> =
        searchRepo()
            .zipWith(reduceBody())
            .map { (repo, dataBuff) ->
                dataBuff.asInputStream().use { service.execute(this, repo, emptyList(), it) }
            }
            .map { it.use { s -> (s as ByteArrayOutputStream).toByteArray() } }
            .flatMap {
                commonHeaders(contentType = service.cmd.responseMediaType).bodyValue(it)
            }
}

