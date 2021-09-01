package com.minus.git.reactive.backend

import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.pack.PackConfig
import org.eclipse.jgit.transport.ReceivePack
import org.eclipse.jgit.transport.UploadPack
import org.eclipse.jgit.transport.resolver.ReceivePackFactory
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.eclipse.jgit.transport.resolver.UploadPackFactory
import org.springframework.stereotype.Component


@Component
class ReactiveUploadPackFactory : UploadPackFactory<ProtocolServiceExecutor> {
    private val timeout: Int = 0
    var packConfig: PackConfig? = null

    @Throws(ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    override fun create(req: ProtocolServiceExecutor, db: Repository): UploadPack {
        val up = UploadPack(db)
        up.timeout = timeout
        up.setPackConfig(packConfig)

        return up
    }
}

@Component
class ReactiveReceivePackFactory : ReceivePackFactory<ProtocolServiceExecutor> {
    @Throws(ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    override fun create(executor: ProtocolServiceExecutor, repo: Repository): ReceivePack {
        val host = "localhost"
        val name = "anonymous"
        val email = "$name@$host"
        return ReceivePack(repo).apply {
            refLogIdent = PersonIdent(name, email)
            timeout = 0
        }
    }
}