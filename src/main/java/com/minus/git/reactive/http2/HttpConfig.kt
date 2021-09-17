package com.minus.git.reactive.http2

import io.netty.handler.codec.http2.Http2SecurityUtil
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.SupportedCipherSuiteFilter
import io.netty.handler.ssl.util.SelfSignedCertificate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
open class HttpConfig {
    @Bean
    open fun sslContext(resourceLoader: ResourceLoader): SslContext {
//        Thread.currentThread().contextClassLoader.run {
//            getResourceAsStream("classpath:")
//            SslContextBuilder.forServer(
//                getResource("server.jks"),
//                getResource("key.pem")
//            )
//        }

        val ssc = SelfSignedCertificate()
//        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
        return SslContextBuilder.forServer(
            resourceLoader.getResource("classpath:28236634_localhost.cert").file,
            resourceLoader.getResource("classpath:28236634_localhost.key").file
        )
            .sslProvider(SslProvider.JDK)

            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            .applicationProtocolConfig(
                ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1
                )
            )
            .build()
    }

}