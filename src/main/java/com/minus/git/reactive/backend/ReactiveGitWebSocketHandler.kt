package com.minus.git.reactive.backend

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID
import java.util.function.BiFunction

data class WebsocketEvent(val id: String, val dt: String)

@Component
class ReactiveGitWebSocketHandler : WebSocketHandler {
    private val eventFlux = Flux.generate<String> { sink ->
        val event = WebsocketEvent(randomUUID().toString(), now().toString())
        try {
            sink.next(json.writeValueAsString(event))
        } catch (e: JsonProcessingException) {
            sink.error(e)
        }
    }

    private val intervalFlux = Flux.interval(Duration.ofMillis(1000L))
        .zipWith(eventFlux, BiFunction { time: Long, event: String -> event })

    override fun handle(webSocketSession: WebSocketSession): Mono<Void> {
        return webSocketSession.send(intervalFlux.map { webSocketSession.textMessage(it) })
            .and(webSocketSession.receive().map<String> { it.payloadAsText })
            .log()
    }

    companion object {
        private val json = ObjectMapper()
    }
}