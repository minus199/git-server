package com.minus.git.reactive.backend


import com.minus.git.reactive.GitV1Enabled
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.ServiceMayNotContinueException
import org.eclipse.jgit.transport.resolver.RepositoryResolver
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import java.net.Socket

@GitV1Enabled
@Component
open class Daemon(
    private val repositoryResolver: RepositoryResolver<ServerRequest>,
    private val services: SupportedServices
) {
    internal fun startClient(socket: Socket) {
        /*ProtocolServiceExecutor(this, socket).use {
            try {
                it.executeService()
            } catch (e: ServiceNotEnabledException) {
                // Ignored. Client cannot use this repository.
            } catch (e: ServiceNotAuthorizedException) {
                // Ignored. Client cannot use this repository.
            } catch (e: IOException) {
                // Ignore unexpected IO exceptions from clients
            }
        }*/
    }

    @Throws(ServiceMayNotContinueException::class)
    internal fun openRepository(client: ServerRequest, name: String): Repository? {
        return try {
            repositoryResolver.open(client, name)
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
