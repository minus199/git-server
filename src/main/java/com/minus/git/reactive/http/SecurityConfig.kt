package com.minus.git.reactive.http

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


@Configuration
open class SecurityConfig {
    private val STOREPASS = "secret".toCharArray()

    @Bean
    open fun javaxSslContext(): SSLContext = SSLContext.getInstance("TLS").apply {
        val trustStore = KeyStore.getInstance("jks")

        Thread.currentThread().contextClassLoader.getResourceAsStream("certs/server/server.jks").use {
            trustStore.load(it, STOREPASS)
        }

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore, STOREPASS)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }
        init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())
    }
}