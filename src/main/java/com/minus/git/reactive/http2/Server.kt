package com.minus.git.reactive.http2

import com.minus.git.reactive.Http2Enabled
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler
import io.netty.handler.ssl.SslContext
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Scope
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

//
//@Http2Enabled
//@Component
//@Scope("prototype")
class HTTP2NegotiationHandler : ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_2) {
    @Throws(Exception::class)
    override fun configurePipeline(ctx: ChannelHandlerContext, protocol: String) {
        if (ApplicationProtocolNames.HTTP_2 == protocol) {
            ctx.pipeline()
                .addLast(
                    Http2FrameCodecBuilder.forServer()
                        .build(), Http2ServerResponseHandler()
                )
            return
        }
        throw IllegalStateException("Protocol: $protocol not supported")
    }
}

@Http2Enabled
@Component
@Scope("prototype")
class SocketChannelInitializer(
    private val sslCtx: SslContext
) : ChannelInitializer<SocketChannel>() {
    @Throws(Exception::class)
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline()
            .addLast(sslCtx.newHandler(ch.alloc()), HTTP2NegotiationHandler())
    }
}

@Http2Enabled
@Component
class BootHttp2Server(private val server: Http2Server) : ApplicationListener<ApplicationReadyEvent> {
    @EventListener
    override fun onApplicationEvent(event: ApplicationReadyEvent) = server.run()
}

@Http2Enabled
@Component
class Http2Server(private val channelInitializer: ChannelInitializer<*>) {
    private val port = 8443
    private val logger = LoggerFactory.getLogger(Http2Server::class.java)
    private val eventLoopGroup: EventLoopGroup = NioEventLoopGroup()

    fun run() {
        try {
            val ch = ServerBootstrap().run {
                option(ChannelOption.SO_BACKLOG, 1024)
                group(eventLoopGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .handler(LoggingHandler(LogLevel.INFO))
                    .childHandler(channelInitializer)

                bind(port)
                    .sync()
                    .channel()
            }
            logger.info("HTTP/2 Server is listening on https://127.0.0.1:$port/")


            ch.closeFuture().sync()
        } finally {
            eventLoopGroup.shutdownGracefully()
        }
    }
}