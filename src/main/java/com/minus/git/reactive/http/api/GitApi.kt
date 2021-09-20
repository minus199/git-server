package com.minus.git.reactive.http.api

import com.minus.git.reactive.backend.GitConst
import com.minus.git.reactive.backend.SupportedServices
import com.minus.git.reactive.service.GitRepositoriesService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono

@Component
class GitApi(
    override val repositoriesService: GitRepositoriesService,
    override val services: SupportedServices
) : RepoRequestOpx {

    fun infoRefs(req: ServerRequest): Mono<ServerResponse> =
        req.searchRepo()
            .flatMapIterable { it.refDatabase.refs }
            .reduce("") { acc, ref -> acc + "${ref.objectId.name} ${ref.name}".pktLine() }
            .map { payload -> arrayOf("# service=${req.requestedGitService().slug}".pktLine(), payload) }
            .map { parts -> parts.joinToString("") { it.pktFlush() } }
            .flatMap { payload ->
                commonHeaders()
                    .contentType(req.requestedGitService().mediaType)
                    .body(BodyInserters.fromValue(payload))
            }

    fun head(req: ServerRequest): Mono<ServerResponse> = status(HttpStatus.NOT_IMPLEMENTED).build()

    fun uploadPack(req: ServerRequest): Mono<ServerResponse> =
        GitConst.Cmd.GIT_UPLOAD_PACK.matchService().flatMap { req.executeService(it) }

    fun receivePack(req: ServerRequest): Mono<ServerResponse> =
        GitConst.Cmd.GIT_RECEIVE_PACK.matchService().flatMap { req.executeService(it) }

    fun textFile(req: ServerRequest): Mono<ServerResponse> = status(HttpStatus.NOT_IMPLEMENTED).build()

    fun infoPacks(req: ServerRequest): Mono<ServerResponse> = status(HttpStatus.NOT_IMPLEMENTED).build()

    fun looseObject(req: ServerRequest): Mono<ServerResponse> = status(HttpStatus.NOT_IMPLEMENTED).build()

    fun packFile(req: ServerRequest): Mono<ServerResponse> = status(HttpStatus.NOT_IMPLEMENTED).build()

    fun idxFile(req: ServerRequest): Mono<ServerResponse> = status(HttpStatus.NOT_IMPLEMENTED).build()

    /* fun uploadPack1(req: ServerRequest): Mono<Flux<InputStream>> =
         req.bodyToMono(String::class.java).map {
             req.exchange().response.bufferFactory().allocateBuffer().asOutputStream()
             req.exchange().request.body.map { db -> db.asInputStream() }
         }

     fun uploadPack3(req: ServerRequest): Mono<ServerResponse> {
         val repo = req.searchRepo().block()!!
         val outputStream = ByteArrayOutputStream()
         return req.exchange().request.body
             .reduce { acc, db -> acc.write(db) }
             .map { it.asInputStream() }
             .zipWith(services.match(GitConst.Cmd.GIT_UPLOAD_PACK))
             .map { (ins, service) ->
                 service.execute(req, repo, emptyList(), ins)
             }
             .flatMap {
                 commonHeaders().bodyValue(String((it as ByteArrayOutputStream).toByteArray()))
             }
     }

     fun uploadPack2(req: ServerRequest): Mono<ServerResponse> =
         req.bodyToMono(String::class.java).flux()
             .flatMap { Flux.fromIterable(it.split("\n")) }
             .mapNotNull<MatchResult>
             { Patterns.fetch.matchEntire(it) }
             .reduce("")
             { payload, matches ->
                 val (none, len, cmd, xtraArg) = matches.groups.map { it?.value ?: "" }
                 payload
             }.flatMap {
                 commonHeaders().build()
             }*/

    /*
    fun receivePack2(req: ServerRequest): Mono<ServerResponse> {
        val repo = req.searchRepo().block()!!
        return req.exchange().request.body
            .reduce { acc, db -> acc.write(db) }
            .map { it.asInputStream() }
            .map { ins ->
                services.getService("receive-pack")!!.execute(req, repo, emptyList(), ins)
            }
            .flatMap {
                ok().bodyValue(String((it as ByteArrayOutputStream).toByteArray()))
            }

    }*/

    /*
    //        val x = repositoryResolver.open(req, "test3")
    //        services.match("git-upload-pack").execute()
    //        services.match("git-upload-pack")
    //        val call = Git.init().setFs(JGitVFS()).call()
        //        Git(x).branchCreate().setName("dev").call()

        req.queryParam("service")
            .flatMap { it.matchService() }
            .filter { repo.isEnabledFor(it) }
            .ifPresent { srv ->
//                val extraParams = req.queryParam("special-stuff???").map { it.split(",") }.get()
//                srv.execute(req, repo, extraParams)
            }
*/
}