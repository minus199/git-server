package com.minus.git.reactive.http

import com.minus.git.reactive.backend.ProtocolService
import com.minus.git.reactive.backend.SupportedServices
import com.minus.git.reactive.service.GitRepositoriesService
import com.minus.git.reactive.service.RepoName
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToServerSentEvents
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

val expiresOn = {
    LocalDateTime
        .of(1980, 1, 1, 0, 0, 0, 0)
        .toEpochSecond(ZoneOffset.UTC)
}
private const val NULL_CHAR = '\u0000'


@Component
class GitPushHandler(private val repositoriesService: GitRepositoriesService, private val services: SupportedServices) {
    private val fluxRefs: Flux<UUID> = Flux.fromIterable(arrayOfNulls<UUID>(100).map { UUID.randomUUID() })

    internal fun String.matchService(): Optional<ProtocolService> = Optional.ofNullable(services.match(this))

    private val userStream = Flux
        .zip(Flux.interval(Duration.ofMillis(100)), fluxRefs.repeat())
        .map { it.t2 }

    fun allRepos(req: ServerRequest): Flux<RepoName> =
        repositoriesService.all()

    fun serviceRPC(req: ServerRequest) =
        ok().body(fluxRefs)

    fun head(req: ServerRequest) =
        ServerResponse
            .status(101)
            .headers {
//                < HTTP/2 200

                it.upgrade = "websocket"
                it.connection = listOf("Upgrade")
            }.build()

//            .body { outputMessage, context ->
//                DataBufferUtils.readByteChannel()
//                Mono.just("doo")
//
//                outputMessage.writeWith() }

    fun infoRefs(req: ServerRequest): Mono<ServerResponse> {
/*
        val repo = repositoriesService.resolve(req, req.pathVariable("repoName"))

        req.queryParam("service")
            .flatMap { it.matchService() }
            .filter { repo.isEnabledFor(it) }
            .ifPresent { srv ->
//                val extraParams = req.queryParam("special-stuff???").map { it.split(",") }.get()
//                srv.execute(req, repo, extraParams)
            }
*/

//repo.refDatabase.refresh()
//        val refs = repo.refDatabase.refs

        /*Mono.from(req.bodyToMono(String::class.java).flatMap { p ->
                ok().body(fluxRefs)
        })*/
////            b666143f6666326bef28afd14f1b64654b279a4d HEAD\0multi_ack thin-pack side-band side-band-64k ofs-delta shallow deepen-since deepen-not deepen-relative no-progress include-tag multi_ack_detailed allow-tip-sha1-in-want allow-reachable-sha1-in-want no-done symref=HEAD:refs/heads/main filter object-format=sha1 agent=git/github-g2e1970ee4adf


        val contentBody = arrayOf(
            "# service=git-upload-pack",
            "NULL_CHAR",
            """95dcfa3633004da0049d3d0fa03f80589cbcaf31 refs/heads/maint${NULL_CHAR}multi_ack""",
            """d049f6c27a2244e12041955e262a404c7faba355 refs/heads/master""",
            """2cb58b79488a98d2721cea644875a8dd0026b115 refs/tags/v1.0""",
            """a3c2e2402b99163d1d59756e5f207ae21cccba4c refs/tags/v1.0^{}""",
            "NULL_CHAR"
        ).map { line ->
            if (line == "NULL_CHAR") {
                NULL_CHAR
            } else {
                (line.length + 4 + 1)
                    .toString(16)
                    .padStart(4, '0')
                    .let { hex -> "$hex$line\n" }
            }
        }

        return ok()
            .contentType(X_GIT_UPLOAD_PACK_ADVERTISEMENT)
            .headers {
//                it.set("content-security-policy", "default-src 'none'; sandbox")
//                it.expires = expiresOn(){
//
                it.pragma = "no-cache"
                it.cacheControl = "no-cache, max-age=0, must-revalidate"
//                it.vary = listOf("Accept-Encoding")
//                it.set("x-frame-options", "DENY")
            }
//            .body(Mono.just(contentBody.joinToString("")))
            .body(Flux.fromIterable(contentBody))
    }


    /*repositoryResolver.open()
    receivePackService.execute()


    req.body { inputMessage, context ->
        inputMessage.body.reduce(object : InputStream() {
            override fun read() = -1
        }) { s: InputStream, d -> SequenceInputStream(s, d.asInputStream()) }
                .flatMap { inputStream ->
                    val x: Publisher<InputStream> = Flux.just(inputStream, context.serverResponse().get().)

                    context.serverResponse().get().writeAndFlushWith { s: Subscriber<in Publisher<out DataBuffer>>? ->
                        s?.onNext(Publisher {

                        })
                        inputStream.toMono().and(s)


                    }

                }
    }*/


    fun textFile(req: ServerRequest) =
        ok().body(fluxRefs)

    fun infoPacks(req: ServerRequest) =
        ok().body(fluxRefs)

    fun looseObject(req: ServerRequest) =
        ok().body(fluxRefs)

    fun packFile(req: ServerRequest) =
        ok().body(fluxRefs)

    fun idxFile(req: ServerRequest) =
        ok().body(fluxRefs)


    fun findAll(req: ServerRequest) =

        ok().body(fluxRefs)

    fun stream(req: ServerRequest) =
        ok().bodyToServerSentEvents(userStream)

    companion object {
        val X_GIT_LOOSE_OBJECT = MediaType("application", "x-git-loose-object")
        val X_GIT_PACKED_OBJECTS = MediaType("application", "x-git-packed-objects")
        val X_GIT_PACKED_OBJECTS_TOC = MediaType("application", "x-git-packed-objects-toc")
        val X_GIT_RECEIVE_PACK_RESULT = MediaType("application", "x-git-receive-pack-result")
        val X_GIT_UPLOAD_PACK_ADVERTISEMENT = MediaType("application", "x-git-upload-pack-advertisement")

    }
}