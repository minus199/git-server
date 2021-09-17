package com.minus.git.reactive.http

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


@Configuration
open class SecurityConfig(
    @Value("\${gradify.ssl.store-pass}") private val storePass: CharArray,
    @Value("\${gradify.git.server.cert}") val sslJKS: String
) {
    @Bean
    open fun javaxSslContext(): SSLContext = SSLContext.getInstance("TLS").apply {
        val trustStore = KeyStore.getInstance("jks")

        Thread.currentThread().contextClassLoader.getResourceAsStream(sslJKS).use {
            trustStore.load(it, storePass)
        }

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore, storePass)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }
        init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())
    }
}