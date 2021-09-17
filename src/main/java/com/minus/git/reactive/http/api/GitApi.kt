package com.minus.git.reactive.http.api

import com.minus.git.reactive.backend.ProtocolService
import com.minus.git.reactive.backend.SupportedServices
import com.minus.git.reactive.repo.CassandraRepositoryResolver
import com.minus.git.reactive.service.GitRepositoriesService
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

val expiresOn = {
    LocalDateTime
        .of(1980, 1, 1, 0, 0, 0, 0)
        .toEpochSecond(ZoneOffset.UTC)
}
private const val NULL_CHAR = '\u0000'

object Patterns {
    val fetch = """^([0-9]{4})([a-z0-9]*)\s([a-z0-9]*)$""".toRegex()
    val eol = """(0{6})([0-9)]{0,2})([a-z]*)""".toRegex()
}

fun String.padLength() = (length + 4 + 1)
    .toString(16)
    .padStart(4, '0')
    .let { hex -> "$hex$this" }


fun ByteArray.receivePackLine() {
    val len = String(sliceArray(0..3)).toInt(16)
    String(sliceArray(4..len)) // pkt line
    String(sliceArray(len..len + 3)) // magic marker
    String(sliceArray(len + 4..len + 7)) // PACK
    sliceArray(len + 8 until size)  // pack bytes
}

@Component
class GitApi(
    private val repositoriesService: GitRepositoriesService,
    private val services: SupportedServices,
    private val repositoryResolver: CassandraRepositoryResolver
) {
    internal fun String.matchService(): Optional<ProtocolService> = Optional.ofNullable(services.match(this))

    private fun ServerRequest.serviceName(): Optional<String> = queryParam("service")
    private fun ServerRequest.service(): Optional<ProtocolService> = serviceName().map { services.getService(it) }

    fun infoRefs(req: ServerRequest): Mono<ServerResponse> {
//        val x = repositoryResolver.open(req, "test3")
//        services.match("git-upload-pack").execute()
//        services.match("git-upload-pack")
//        val call = Git.init().setFs(JGitVFS()).call()
//        Git(x).branchCreate().setName("dev").call()


        val serviceName =
            req.serviceName().orElseThrow { throw UnsupportedOperationException("Unable to extract service name") }
        val contentType = when (serviceName) {
            "git-upload-pack" -> X_GIT_UPLOAD_PACK_ADVERTISEMENT
            "git-receive-pack" -> X_GIT_RECEIVE_PACK_ADVERTISEMENT
            else -> throw UnsupportedOperationException("service name has no appropriate content-type")
        }
        return ok()
            .contentType(contentType)
            .headers {
                it.pragma = "no-cache"
                it.cacheControl = "no-cache, max-age=0, must-revalidate"
                it.connection = listOf("keep-alive")
            }
            .body(
                BodyInserters.fromValue(
                    """
                    ${"# service=$serviceName".padLength()}
                    00000000
                """.trimIndent()

                )
            )


        return repositoriesService
            .resolve(req.pathVariable("repoName"))
            .flatMapIterable { it.refDatabase.refs }
            .doOnError { println(it) }
            .map { "${it.objectId} ${it.name}" }
            .map { line ->
                (line.length + 4 + 1)
                    .toString(16)
                    .padStart(4, '0')
                    .let { hex -> "$hex$line\n" }
            }
            .reduce("") { payload, line -> "$payload\n$line" }
            .map {
                """
                    # service=git-upload-pack
                    0000
                    $it
                    0000
                """.trimIndent()
            }.flatMap {
                ok()
                    .contentType(X_GIT_UPLOAD_PACK_ADVERTISEMENT)
                    .headers {
                        it.pragma = "no-cache"
                        it.cacheControl = "no-cache, max-age=0, must-revalidate"
                        it.connection = listOf("keep-alive")
                    }
                    .body(BodyInserters.fromValue(it))
            }

/*

        req.queryParam("service")
            .flatMap { it.matchService() }
            .filter { repo.isEnabledFor(it) }
            .ifPresent { srv ->
//                val extraParams = req.queryParam("special-stuff???").map { it.split(",") }.get()
//                srv.execute(req, repo, extraParams)
            }
*/

        /*  val contentBody = arrayOf(
              "# service=git-upload-pack",
              "0000",
  //            """95dcfa3633004da0049d3d0fa03f80589cbcaf31 refs/heads/maint${NULL_CHAR}multi_ack""",
              """d049f6c27a2244e12041955e262a404c7faba355 refs/heads/master""",
              """2cb58b79488a98d2721cea644875a8dd0026b115 refs/tags/v1.0""",
              """a3c2e2402b99163d1d59756e5f207ae21cccba4c refs/tags/v1.0^{}""",
              "0000"
          ).map { line ->
              if (line == "0000") line
              else (line.length + 4 + 1)
                  .toString(16)
                  .padStart(4, '0')
                  .let { hex -> "$hex$line\n" }
          }

          return ok()
              .contentType(X_GIT_UPLOAD_PACK_ADVERTISEMENT)
              .headers {
                  it.pragma = "no-cache"
                  it.cacheControl = "no-cache, max-age=0, must-revalidate"
                  it.connection = listOf("keep-alive")
              }
              .body(BodyInserters.fromValue(contentBody.joinToString("")))*/
    }


    /*repositoryResolver.open()
    receivePackService.execute()


    req.body { inputMessage, context ->
        inputMessage.body.reduce(object : InputStream() {
            override fun read() = -1
        }) { s: InputStream, d -> SequenceInputStream(s, d.asInputStream()) }
                .flatMap { inputStream ->
                    val x: Publisher<InputStream> = Flux.just(inputStream, context.serverResponse().get().)

                    context.serverResponse().get().writeAndFlushWith { s: Subscriber<in Publisher<out DataBuffer>>? ->
                        s?.onNext(Publisher {

                        })
                        inputStream.toMono().and(s)


                    }

                }
    }*/


    fun head(req: ServerRequest): Mono<ServerResponse> = ServerResponse
        .status(101)
        .headers {
            //                < HTTP/2 200
            it.upgrade = "websocket"
            it.connection = listOf("Upgrade")
        }.build()


    fun uploadPack2(req: ServerRequest) =
        req.bodyToMono(String::class.java).map {
            req.exchange().response.bufferFactory().allocateBuffer().asOutputStream()
            req.exchange().request.body.map { db -> db.asInputStream() }
        }

    fun uploadPack(req: ServerRequest): Mono<ServerResponse> =
        req.bodyToMono(String::class.java).flux()
            .flatMap { Flux.fromIterable(it.split("\n")) }
            .mapNotNull<MatchResult> { Patterns.fetch.matchEntire(it) }
            .reduce("") { payload, matches ->
                val (none, len, cmd, xtraArg) = matches.groups.map { it?.value ?: "" }
                payload
            }.flatMap {
                ok().build()
            }

    fun receivePack(req: ServerRequest): Mono<ServerResponse> {
        val repo = repositoryResolver.open(req, "empty1")
        val outputStream = ByteArrayOutputStream()
        return req.exchange().request.body
            .reduce { acc, db -> acc.write(db) }
            .map { it.asInputStream() }
            .map { ins ->
                services.getService("receive-pack")!!.execute(req, repo, emptyList(), ins, outputStream)
                outputStream
            }
            .flatMap {
                val x = String(it.toByteArray())
                ok().build()
            }

    }

    fun textFile(req: ServerRequest) =
        ok().build()

    fun infoPacks(req: ServerRequest) =
        ok().build()

    fun looseObject(req: ServerRequest) =
        ok().build()

    fun packFile(req: ServerRequest) =
        ok().build()

    fun idxFile(req: ServerRequest) =
        ok().build()


    fun findAll(req: ServerRequest): Mono<ServerResponse> =

        ok().build()

    fun stream(req: ServerRequest) =
//        ok().bodyToServerSentEvents(userStream)
        ok().build()

    companion object {
        val X_GIT_LOOSE_OBJECT = MediaType("application", "x-git-loose-object")
        val X_GIT_PACKED_OBJECTS = MediaType("application", "x-git-packed-objects")
        val X_GIT_PACKED_OBJECTS_TOC = MediaType("application", "x-git-packed-objects-toc")
        val X_GIT_RECEIVE_PACK_RESULT = MediaType("application", "x-git-receive-pack-result")
        val X_GIT_UPLOAD_PACK_ADVERTISEMENT = MediaType("application", "x-git-upload-pack-advertisement")
        val X_GIT_RECEIVE_PACK_ADVERTISEMENT = MediaType("application", "x-git-receive-pack-advertisement")

    }
}