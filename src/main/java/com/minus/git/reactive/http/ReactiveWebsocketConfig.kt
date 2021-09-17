package com.minus.git.reactive.http

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
open class ReactiveWebsocketConfig(private val gitWsHandler: ReactiveGitWebSocketHandler) {

    @Bean
    open fun webSocketHandlerMapping(): HandlerMapping = SimpleUrlHandlerMapping().apply {
        order = Ordered.HIGHEST_PRECEDENCE
        urlMap = mapOf<String, WebSocketHandler>("/foo-socket" to gitWsHandler)
    }

    @Bean
    open fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter().apply {
        order = Ordered.HIGHEST_PRECEDENCE
    }
}