package com.minus.git.reactive.http.api

import com.minus.git.reactive.repo.CassandraRepositoryResolver
import com.minus.git.reactive.service.GitRepositoriesService
import com.minus.git.reactive.service.RepoName
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

@Component
class RepositoriesApi(
    private val repositoriesService: GitRepositoriesService
) {
    private val fluxRefs: Flux<UUID> = Flux.fromIterable(arrayOfNulls<UUID>(100).map { UUID.randomUUID() })
    private val userStream = Flux
        .zip(Flux.interval(Duration.ofMillis(100)), fluxRefs.repeat())
        .map { it.t2 }

    fun t() {

    }

    fun create(req: ServerRequest): Mono<ServerResponse> =
        repositoriesService.create(req.pathVariable("repoName"))
            .map { it.description.repositoryName }
            .flatMap {
                ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(it))
            }


    fun allRepos(req: ServerRequest): Flux<RepoName> = repositoriesService.all()
}