package com.minus.git.reactive.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.minus.git.reactive.ReactiveEnabled
import io.undertow.UndertowOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.undertow.UndertowReactiveWebServerFactory
import org.springframework.boot.web.server.Compression
import org.springframework.boot.web.server.Http2
import org.springframework.boot.web.server.Ssl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Mono
import javax.net.ssl.SSLContext


@ReactiveEnabled
@Configuration
@EnableWebFlux
open class ReactiveBackendConfig(
    private val gitWsHandler: ReactiveGitWebSocketHandler,
    private val jackson: ObjectMapper
) : WebFluxConfigurer {
    @Bean
    open fun gitHttpWebHandler(@Autowired routes: Routes): HttpHandler =
        HandlerStrategies.builder()
            .exceptionHandler { exchange, ex ->
                exchange.response.let { response ->
                    response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                    response.headers.contentType = MediaType.APPLICATION_JSON
                    response.bufferFactory().let { bufferFactory ->
                        ApiException(ex.message ?: "Internal server error", response.statusCode!!)
                            .let { ex -> jackson.writeValueAsBytes(ex) }
                            .let { bufferFactory.wrap(it) }
                            .let { dataBuffer -> response.writeWith(Mono.just(dataBuffer)) }
                    }

                }
            }
            .build()
            .run { RouterFunctions.toHttpHandler(routes.router(), this) }

    @Bean
    open fun reactorServerResourceFactory(): ReactorResourceFactory = ReactorResourceFactory()

    @Bean
    open fun embeddedServletContainerFactory(
        httpHandler: HttpHandler,
        javaxSslContext: SSLContext
    ): UndertowReactiveWebServerFactory? {
        val factory = UndertowReactiveWebServerFactory().apply {
//            http2 = Http2().apply { isEnabled = true }
            compression = Compression().apply { enabled = false }

            addBuilderCustomizers({
                it
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .setServerOption(UndertowOptions.HTTP2_SETTINGS_ENABLE_PUSH, true)
                    .addHttpsListener(6480, "localhost", javaxSslContext)
            })

//            ssl = Ssl().apply {
//                isEnabled = true
//                keyStore = "classpath:certs/server/server.jks"
//                keyStorePassword = "secret"
//            }

            CorsRegistry().apply {
                addMapping("*")
                addCorsMappings(this)
            }

            //        setResourceFactory(resourceFactory)
            getWebServer(httpHandler)
        }


        return factory
    }

    //    @Bean
    open fun nettyReactiveWebServerFactory(
        httpHandler: HttpHandler,
        @Qualifier("reactorServerResourceFactory") resourceFactory: ReactorResourceFactory
    ): NettyReactiveWebServerFactory = NettyReactiveWebServerFactory().apply {
        port = 6480
        compression = Compression().apply { enabled = false }
        http2 = Http2().apply { isEnabled = true }
        ssl = Ssl().apply {
            isEnabled = true
            keyStore = "classpath:certs/server/server.jks"
            keyStorePassword = "secret"
        }

        CorsRegistry().apply {
            addMapping("*")
            addCorsMappings(this)
        }

        //        setResourceFactory(resourceFactory)
        getWebServer(httpHandler)
    }

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