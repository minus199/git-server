package com.minus.git.reactive.backend

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.PacketLineOut
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.eclipse.jgit.transport.resolver.ReceivePackFactory
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.eclipse.jgit.transport.resolver.UploadPackFactory
import org.springframework.stereotype.Component
import java.io.IOException

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

@Component
class ReceivePackProtocolService(private val receivePackFactory: ReceivePackFactory<ProtocolServiceExecutor>) :
    ProtocolService("receive-pack", "receivepack") {

    override fun ProtocolServiceExecutor.execute(repo: Repository, extraParameters: Collection<String>?) {
        receivePackFactory.create(this, repo).apply {
            receive(inputStream, outputStream, null)
        }
    }
}

@Component
class UploadPackProtocolService(private val uploadPackFactory: UploadPackFactory<ProtocolServiceExecutor>) :
    ProtocolService("upload-pack", "uploadpack") {

    override fun ProtocolServiceExecutor.execute(repo: Repository, extraParameters: Collection<String>?) {
        val up = uploadPackFactory.create(this, repo)
        if (extraParameters != null) {
            up.setExtraParameters(extraParameters)
        }

        up.upload(inputStream, outputStream, null)
    }
}

abstract class ProtocolService internal constructor(cmdName: String, cfgName: String) {
    val commandName: String = if (cmdName.startsWith("git-")) cmdName else "git-$cmdName"
    var isEnabled: Boolean = false
    private val configKey: Config.SectionParser<ServiceConfig>
    private var isOverridable: Boolean = false

    init {
        configKey = Config.SectionParser { cfg -> ServiceConfig(this@ProtocolService, cfg, cfgName) }
        isOverridable = true
    }

    private class ServiceConfig constructor(service: ProtocolService, cfg: Config, name: String) {
        val enabled: Boolean = cfg.getBoolean("gitHttpRequest", name, service.isEnabled)
    }

    /**
     * Determine if this service can handle the requested command.
     *
     * @param commandLine
     * input line from the client.
     * @return true if this command can accept the given command line.
     */
    fun String.canHandleCmd(): Boolean {
        return (commandName.length + 1 < length
                && this[commandName.length] == ' '
                && this.startsWith(commandName))
    }

    @Throws(IOException::class, ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    internal fun execute(serviceExecutor: ProtocolServiceExecutor, cmd: String, extraParams: Collection<String>?) {
        val name = cmd.substring(commandName.length + 1)
        try {
            serviceExecutor.daemon.openRepository(serviceExecutor, name).use { repository ->
                if (repository!!.isEnabledFor()) {
//                    if (extraParameters != null) {
                    serviceExecutor.execute(repository, extraParams)
//                    }
                }
            }
        } catch (e: ServiceMayNotContinueException) {
            // An error when opening the repo means the serviceExecutor is expecting a ref
            // advertisement, so use that style of error.
            PacketLineOut(serviceExecutor.outputStream).run {
                writeString("ERR " + e.cause?.message + "\n")
            }
        }
    }

    @Throws(IOException::class, ServiceNotEnabledException::class, ServiceNotAuthorizedException::class)
    internal abstract fun ProtocolServiceExecutor.execute(repo: Repository, extraParameters: Collection<String>?)

    private fun Repository.isEnabledFor(): Boolean {
        return if (isOverridable) config.get(configKey).enabled else isEnabled
    }
}