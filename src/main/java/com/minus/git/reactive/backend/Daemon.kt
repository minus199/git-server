package com.minus.git.reactive.backend


import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.Socket

@Component
open class Daemon(
    private val repositoryResolver: RepositoryResolver<ProtocolServiceExecutor?>,
    private val services: SupportedServices
) {
    internal fun startClient(socket: Socket) {
        ProtocolServiceExecutor(this, socket).use {
            try {
                it.executeService()
            } catch (e: ServiceNotEnabledException) {
                // Ignored. Client cannot use this repository.
            } catch (e: ServiceNotAuthorizedException) {
                // Ignored. Client cannot use this repository.
            } catch (e: IOException) {
                // Ignore unexpected IO exceptions from clients
            }
        }
    }

    @Throws(ServiceMayNotContinueException::class)
    internal fun openRepository(client: ProtocolServiceExecutor, name: String): Repository? {
        // Assume any attempt to use \ was by a Windows client and correct to the more typical / used in Git URIs.
        val sanitizedName = name.replace('\\', '/')

        // git://thishost/path should always be name="/path" here
        if (!sanitizedName.startsWith("/"))
            return null

        return try {
            repositoryResolver.open(client, sanitizedName.substring(1))
        } catch (e: RepositoryNotFoundException) {
            // null signals it "wasn't found", which is all that is suitable
            // for the remote client to know.
            null
        } catch (e: ServiceNotAuthorizedException) {
            // null signals it "wasn't found", which is all that is suitable
            // for the remote client to know.
            null
        } catch (e: ServiceNotEnabledException) {
            // null signals it "wasn't found", which is all that is suitable
            // for the remote client to know.
            null
        }

    }

    @Synchronized
    internal fun String.matchService(): ProtocolService? = services.match(this)
}
