package com.minus.git.reactive.http

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

interface GradifyApiExceptionsHandler : WebExceptionHandler {
    fun ServerWebExchange.bufferedResponse(
        contentType: MediaType = MediaType.APPLICATION_JSON,
        status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        errHandler: (ServerHttpResponse, DataBufferFactory) -> Mono<DataBuffer>
    ): Mono<Void> = errHandler(response, response.bufferFactory()).let {
        response.statusCode = status
        response.headers.contentType = contentType
        response.writeWith(it)
    }
}

@Component
class GeneralWebExceptionsHandler(private val jackson: ObjectMapper) : GradifyApiExceptionsHandler {
    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> =
        exchange.bufferedResponse { response, bufferFactory ->
            ApiException(ex.message ?: "Internal server error", response.statusCode!!)
                .let { ex -> jackson.writeValueAsBytes(ex) }
                .let { bufferFactory.wrap(it) }.toMono()
        }
}

@Component
class GitWebExceptionsHandler : GradifyApiExceptionsHandler {
    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> =
        exchange.bufferedResponse(MediaType.TEXT_PLAIN, HttpStatus.NOT_FOUND) { response, bufferFactory ->
            when (ex) {
                is RepositoryNotFoundException, is ServiceMayNotContinueException -> {
                    bufferFactory
                        .wrap((ex.message ?: ex::class.java.name).toByteArray())
                        .toMono()
                }
                else -> Mono.empty()
            }
        }
}