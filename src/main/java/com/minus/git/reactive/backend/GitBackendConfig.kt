package com.minus.git.reactive.backend

import com.fasterxml.jackson.databind.ObjectMapper
import com.minus.git.reactive.http.ApiException
import com.minus.git.reactive.http.Routes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Mono


@Configuration
@EnableWebFlux
open class GitBackendConfig(private val gitWsHandler: ReactiveGitWebSocketHandler, private val jackson: ObjectMapper) :
    WebFluxConfigurer {
    @Bean
    open fun gitHttpWebHandler(@Autowired routes: Routes): HttpHandler {
        val strategies = HandlerStrategies.builder()
            /*.webFilter { exchange, chain ->
                chain.filter(exchange)
            }*/
            /*
              .viewResolver(ViewResolver { viewName, locale ->  })*/
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

            /*.codecs { config: ServerCodecConfigurer ->
                config.readers.add(object : HttpMessageReader<String> {
                    override fun canRead(elementType: ResolvableType, mediaType: MediaType?): Boolean {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun readMono(elementType: ResolvableType, message: ReactiveHttpInputMessage, hints: MutableMap<String, Any>): Mono<String> {
                        return message.body.map { buff ->
                            val res = arrayListOf<String>()
                            val index = buff.readableByteCount()
                            String(0.rangeTo(index).map { buff.read() }.toByteArray())
                        }.toMono()
                    }

                    override fun getReadableMediaTypes(): MutableList<MediaType> {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun read(elementType: ResolvableType, message: ReactiveHttpInputMessage, hints: MutableMap<String, Any>): Flux<String> {
                        return message.body.flatMap { buff ->
                            do {
                                String()
                            } while (buff.readableByteCount() > 0)

                            Flux.fromIterable(bigIntegers)
                        }
                    }
                })
            }*/

            .build()

        return RouterFunctions.toHttpHandler(routes.router(), strategies)
    }

    @Bean
    open fun webSocketHandlerMapping(): HandlerMapping {
        val map = HashMap<String, WebSocketHandler>()
        map["/event-emitter"] = gitWsHandler

        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.order = 1
        handlerMapping.urlMap = map
        return handlerMapping
    }

    @Bean
    open fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

    @Bean
    open fun reactorServerResourceFactory(): ReactorResourceFactory {
        return ReactorResourceFactory()
    }

    @Bean
    open fun nettyReactiveWebServerFactory(
        httpHandler: HttpHandler,
        @Qualifier("reactorServerResourceFactory") resourceFactory: ReactorResourceFactory
    ): NettyReactiveWebServerFactory {
        val serverFactory = NettyReactiveWebServerFactory()

        serverFactory.port = 6480
        serverFactory.getWebServer(httpHandler)

        serverFactory.setResourceFactory(resourceFactory)
        return serverFactory
    }
}