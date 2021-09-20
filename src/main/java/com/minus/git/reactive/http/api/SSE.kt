package com.minus.git.reactive.http.api

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.UUID

@Component

class SSE {
    private val fluxRefs: Flux<UUID> = Flux.fromIterable(arrayOfNulls<UUID>(100).map { UUID.randomUUID() })
    private val userStream = Flux
        .zip(Flux.interval(Duration.ofMillis(100)), fluxRefs.repeat())
        .map { it.t2 }



    fun stream(req: ServerRequest) =
//        ok().bodyToServerSentEvents(userStream)
        ServerResponse.ok().build()

}