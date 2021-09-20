package com.minus.git.reactive.http.api

import com.minus.git.reactive.repo.branches
import com.minus.git.reactive.repo.commits
import com.minus.git.reactive.service.GitRepositoriesService
import com.minus.git.reactive.service.RepoName
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@Component
class RepositoriesApi(override val repositoriesService: GitRepositoriesService) : RepoApiRequestOpx {
    fun allRepos(req: ServerRequest): Flux<RepoName> = repositoriesService.all()

    fun create(req: ServerRequest): Mono<ServerResponse> =
        repositoriesService.create(req.pathVariable("repoName"))
            .map { it.description.repositoryName }
            .flatMap { ServerResponse.created(req.uri()).build() }

    fun commits(req: ServerRequest): Mono<ServerResponse> =
        req.searchRepo()
            .flatMapMany { repo ->
                repo.commits(req.pathVariable("branch"))
            }.map { it.fullMessage }
            .`as` {
                ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromProducer(it, String::class.java))
            }

    fun branches(req: ServerRequest): Mono<ServerResponse> =
        req.searchRepo()
            .flatMapMany { repo ->
                repo.branches().map { it.name }
            }
            .`as` {
                ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromProducer(it, String::class.java))
            }

    fun repoInfo(req: ServerRequest): Mono<ServerResponse> = throw UnsupportedOperationException()

    fun delete(req: ServerRequest): Mono<ServerResponse> = throw UnsupportedOperationException()
}