package com.minus.git.reactive.backend

import com.minus.git.reactive.ReactiveEnabled
import com.minus.git.reactive.http.api.ServerRequestOpx
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.resolver.ReceivePackFactory
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.eclipse.jgit.transport.resolver.UploadPackFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.kotlin.core.publisher.toMono
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


@Component
class SupportedServices(private val registered: Map<String, ProtocolService>) {
    @Synchronized
    fun match(cmd: GitConst.Cmd) = registered
        .getOrElse(cmd.slug) { throw UnsupportedGitCmdException(cmd) }
        .toMono()
}

abstract class ProtocolService internal constructor(val cmd: GitConst.Cmd) : ServerRequestOpx {
    var isOverridable: Boolean = false

    open val isEnabled: Boolean
        get() = false

    val configKey: Config.SectionParser<ServiceConfig> = Config.SectionParser { cfg ->
        ServiceConfig(this@ProtocolService, cfg, cmd.cfgName)
    }

    class ServiceConfig constructor(service: ProtocolService, cfg: Config, name: String) {
        val enabled: Boolean = cfg.getBoolean("gitHttpRequest", name, service.isEnabled)
    }

    @Throws(IOException::class, ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    internal abstract fun execute(
        request: ServerRequest,
        repo: Repository,
        extraParameters: Collection<String>,
        inputStream: InputStream
    ): OutputStream
}

@Component("git-receive-pack")
@ReactiveEnabled
class ReceivePackProtocolService(private val receivePackFactory: ReceivePackFactory<ServerRequest>) :
    ProtocolService(GitConst.Cmd.GIT_RECEIVE_PACK) {
    override val isEnabled
        get() = true

    override fun execute(
        request: ServerRequest,
        repo: Repository,
        extraParameters: Collection<String>,
        inputStream: InputStream
    ) = ByteArrayOutputStream().apply {
        receivePackFactory.create(request, repo)
            .apply {
                isBiDirectionalPipe = false
            }
            .receive(inputStream, this, null)
    }
}

@Component("git-upload-pack")
@ReactiveEnabled
class UploadPackProtocolService(private val uploadPackFactory: UploadPackFactory<ServerRequest>) :
    ProtocolService(GitConst.Cmd.GIT_UPLOAD_PACK) {
    override val isEnabled
        get() = true

    override fun execute(
        request: ServerRequest,
        repo: Repository,
        extraParameters: Collection<String>,
        inputStream: InputStream
    ) = ByteArrayOutputStream().apply {
        uploadPackFactory
            .create(request, repo)
            .apply {
                setExtraParameters(extraParameters)
                isBiDirectionalPipe = false
            }
            .upload(inputStream, this, null)
    }
}
