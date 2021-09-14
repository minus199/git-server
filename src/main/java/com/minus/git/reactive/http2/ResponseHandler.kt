package com.minus.git.reactive.http2

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.util.CharsetUtil
import org.springframework.stereotype.Component

@Component
@Sharable
class Http2ServerResponseHandler : ChannelDuplexHandler() {
    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.fireExceptionCaught(cause)
        cause.printStackTrace()
        ctx.close()
    }

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is Http2HeadersFrame) {
            val msgHeader = msg
            if (msgHeader.isEndStream) {
                val content = ctx.alloc()
                    .buffer()
                content.writeBytes(RESPONSE_BYTES.duplicate())
                val headers = DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText())
                ctx.write(DefaultHttp2HeadersFrame(headers).stream(msgHeader.stream()))
                ctx.write(DefaultHttp2DataFrame(content, true).stream(msgHeader.stream()))
            }
        } else {
            super.channelRead(ctx, msg)
        }
    }

    @Throws(Exception::class)
    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    companion object {
        val RESPONSE_BYTES = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello World", CharsetUtil.UTF_8))
    }
}