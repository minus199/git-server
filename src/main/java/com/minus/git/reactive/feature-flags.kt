package com.minus.git.reactive

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty(
    prefix = "gradify.http2",
    name = ["isEnabled"],
    matchIfMissing = false,
    havingValue = "true"
)
annotation class Http2Enabled()

@ConditionalOnProperty(
    prefix = "gradify.reactive-server",
    name = ["isEnabled"],
    matchIfMissing = false,
    havingValue = "true"
)
annotation class ReactiveEnabled()

@ConditionalOnProperty(
    prefix = "gradify.git-server",
    name = ["isEnabled"],
    matchIfMissing = false,
    havingValue = "true"
)
annotation class GitV1Enabled()