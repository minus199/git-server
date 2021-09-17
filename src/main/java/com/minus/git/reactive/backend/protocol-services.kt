package com.minus.git.reactive.backend

import com.minus.git.reactive.ReactiveEnabled
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.resolver.ReceivePackFactory
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.eclipse.jgit.transport.resolver.UploadPackFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


@Component
class SupportedServices(private val services: Array<ProtocolService>) {
    @Synchronized
    fun match(cmd: String) = services.find { it.run { cmd.canHandleCmd() } }

    @Synchronized
    fun getService(name: String): ProtocolService? {
        val serviceName = if (!name.startsWith("git-")) "git-$name" else name
        return services.find { it.commandName == serviceName }
    }
}

abstract class ProtocolService internal constructor(cmdName: String, cfgName: String) {
    val commandName: String = if (cmdName.startsWith("git-")) cmdName else "git-$cmdName"
    open val isEnabled: Boolean
        get() = false

    val configKey: Config.SectionParser<ServiceConfig>
    var isOverridable: Boolean = false

    init {
        configKey = Config.SectionParser { cfg -> ServiceConfig(this@ProtocolService, cfg, cfgName) }
        isOverridable = true
    }

    class ServiceConfig constructor(service: ProtocolService, cfg: Config, name: String) {
        val enabled: Boolean = cfg.getBoolean("gitHttpRequest", name, service.isEnabled)
    }

    fun String.canHandleCmd(): Boolean = commandName == this

    @Throws(IOException::class, ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    internal abstract fun execute(
        request: ServerRequest,
        repo: Repository,
        extraParameters: Collection<String>,
        inputStream: InputStream,
        outputStream: OutputStream
    )
}

@Component
@ReactiveEnabled
class ReceivePackProtocolService(private val receivePackFactory: ReceivePackFactory<ServerRequest>) :
    ProtocolService("receive-pack", "receivepack") {
    override val isEnabled
        get() = true

    override fun execute(
        request: ServerRequest,
        repo: Repository,
        extraParameters: Collection<String>,
        inputStream: InputStream,
        outputStream: OutputStream
    ) = receivePackFactory.create(request, repo).receive(inputStream, outputStream, null)
}

@Component
@ReactiveEnabled
class UploadPackProtocolService(private val uploadPackFactory: UploadPackFactory<ServerRequest>) :
    ProtocolService("upload-pack", "uploadpack") {
    override val isEnabled
        get() = true

    override fun execute(
        request: ServerRequest,
        repo: Repository,
        extraParameters: Collection<String>,
        inputStream: InputStream,
        outputStream: OutputStream
    ) = uploadPackFactory
        .create(request, repo)
        .apply { setExtraParameters(extraParameters) }
        .upload(inputStream, outputStream, null)
}
