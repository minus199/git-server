package com.minus.git.reactive.http

import com.minus.git.reactive.ReactiveEnabled
import io.undertow.UndertowOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.undertow.UndertowReactiveWebServerFactory
import org.springframework.boot.web.server.Compression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions
import javax.net.ssl.SSLContext

@ReactiveEnabled
@Configuration
@EnableWebFlux
open class ReactiveBackendConfig(
    @Value("\${gradify.git.server.host}") private val host: String,
    @Value("\${gradify.git.server.port}") private val httpsPort: Int
) : WebFluxConfigurer {

    @Bean
    open fun gitHttpWebHandler(routes: Routes, exceptionHandlers: List<GradifyApiExceptionsHandler>): HttpHandler =
        HandlerStrategies.builder()
            .apply { exceptionHandlers.forEach { exceptionHandler(it) } }
            .build()
            .run { RouterFunctions.toHttpHandler(routes.rootRouter(), this) }

    @Bean
    open fun serverFactory(httpHandler: HttpHandler, javaxSslContext: SSLContext): UndertowReactiveWebServerFactory =
        UndertowReactiveWebServerFactory().apply {
            //        setResourceFactory(resourceFactory)
            compression = Compression().apply { enabled = true }
            CorsRegistry().apply {
                addMapping("*")
                addCorsMappings(this)
            }

            addBuilderCustomizers({
                it
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .setServerOption(UndertowOptions.HTTP2_SETTINGS_ENABLE_PUSH, true)
                    .addHttpsListener(httpsPort, host, javaxSslContext)
            })

            getWebServer(httpHandler)
        }

    @Bean
    open fun reactorServerResourceFactory(): ReactorResourceFactory = ReactorResourceFactory()
}