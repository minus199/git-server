package com.minus.git.reactive.backend

import com.minus.git.reactive.ReactiveEnabled
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
import org.springframework.web.reactive.function.server.ServerRequest
import java.io.InputStream
import java.io.OutputStream

class ReactiveUploadPack(repo: Repository, val host: String = "localhost", val name: String = "anonymous") :
    UploadPack(repo){

    }

class ReactiveReceivePack(repo: Repository, val host: String = "localhost", val name: String = "anonymous"):
    ReceivePack(repo){
    override fun receive(input: InputStream?, output: OutputStream?, messages: OutputStream?) {
        super.receive(input, output, messages)
    }

}

@ReactiveEnabled
@Component
class ReactiveUploadPackFactory : UploadPackFactory<ServerRequest> {
    private val timeout: Int = 0
    var packConfig: PackConfig? = null

    @Throws(ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    override fun create(req: ServerRequest, db: Repository): UploadPack = UploadPack(db).apply {
        timeout = this@ReactiveUploadPackFactory.timeout
        setPackConfig(packConfig)
    }
}

@ReactiveEnabled
@Component
class ReactiveReceivePackFactory : ReceivePackFactory<ServerRequest> {
    @Throws(ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    override fun create(req: ServerRequest, repo: Repository): ReceivePack = ReceivePack(repo).apply {
        val host = "localhost"
        val name = "anonymous"
        val email = "$name@$host"

        refLogIdent = PersonIdent(name, email)
        timeout = 0
    }
}