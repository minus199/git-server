package com.minus.git.reactive.http

import com.minus.git.reactive.service.GitRepositoriesService
import com.minus.git.reactive.service.RepoName
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToServerSentEvents
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.UUID



@Component
class GitPushHandler(private val repositoriesService: GitRepositoriesService) {
    private val fluxRefs: Flux<UUID> = Flux.fromIterable(arrayOfNulls<UUID>(100).map { UUID.randomUUID() })

    private val userStream = Flux
        .zip(Flux.interval(Duration.ofMillis(100)), fluxRefs.repeat())
        .map { it.t2 }

    fun allRepos(req: ServerRequest): Flux<RepoName> =
        repositoriesService.all()

    fun serviceRPC(req: ServerRequest) =
        ok().body(fluxRefs)

    fun head(req: ServerRequest) =
        ok().body(fluxRefs)

    fun infoRefs(req: ServerRequest) =
        /*Mono.from(req.bodyToMono(String::class.java).flatMap { p ->
            ok().body(fluxRefs)
        })*/

        ok().body(fluxRefs)


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

}