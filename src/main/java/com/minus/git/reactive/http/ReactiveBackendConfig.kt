package com.minus.git.reactive.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.minus.git.reactive.ReactiveEnabled
import io.undertow.UndertowOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.undertow.UndertowReactiveWebServerFactory
import org.springframework.boot.web.server.Compression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions
import reactor.core.publisher.Mono
import javax.net.ssl.SSLContext

@ReactiveEnabled
@Configuration
@EnableWebFlux
open class ReactiveBackendConfig(
    private val jackson: ObjectMapper,
    @Value("\${gradify.git.server.host}") private val serverHost: String,
    @Value("\${gradify.git.server.port}") private val serverPort: Int
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
    open fun serverFactory(httpHandler: HttpHandler, javaxSslContext: SSLContext): UndertowReactiveWebServerFactory {
        val factory = UndertowReactiveWebServerFactory().apply {
            compression = Compression().apply { enabled = true }

            addBuilderCustomizers({
                it
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .setServerOption(UndertowOptions.HTTP2_SETTINGS_ENABLE_PUSH, true)
                    .addHttpsListener(serverPort, serverHost, javaxSslContext)
            })

            CorsRegistry().apply {
                addMapping("*")
                addCorsMappings(this)
            }

            //        setResourceFactory(resourceFactory)
            getWebServer(httpHandler)
        }

        return factory
    }

}