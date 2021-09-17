package com.minus.git.reactive.http

import com.minus.git.reactive.http.api.GitApi
import com.minus.git.reactive.http.api.RepositoriesApi
import com.minus.git.reactive.service.RepoName
import org.springframework.http.MediaType.ALL
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Component
class Routes(private val gitApi: GitApi, private val repositoriesApi: RepositoriesApi) {
    fun router(): RouterFunction<ServerResponse> = router {
        "/{repoName}.git".nest {
            accept(ALL).nest {
                GET("", gitApi::head)
                GET("/HEAD", gitApi::head)
                GET("/info/refs", gitApi::infoRefs)

                POST("/git-upload-pack", gitApi::uploadPack)
                POST("/git-receive-pack", gitApi::receivePack)

                GET("/objects/info/alternates", gitApi::textFile)
                GET("/objects/info/http-alternates", gitApi::textFile)
                GET("/objects/info/packs", gitApi::infoPacks)
                GET("/objects/{objGroup:[0-9a-f]\\{2\\}}/{objID: [0-9a-f]\\{38\\}$}", gitApi::looseObject)
                GET("/objects/pack/{packIdPack:pack-[0-9a-f]\\{40\\}\\.pack$}", gitApi::packFile)
                GET("/objects/pack/{packIdx:pack-[0-9a-f]{40}\\.idx$}", gitApi::idxFile)

                GET("/git-push/{foo}", gitApi::findAll)
                POST("/git-push/{foo}", gitApi::findAll)
            }
        }

        "/api".nest {
            "/repositories".nest {
                GET("") { req ->
                    ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .body(repositoriesApi.allRepos(req), RepoName::class.java)
                }

                POST("/{repoName}.git", repositoriesApi::create)
            }

            accept(TEXT_EVENT_STREAM).nest {
                GET("/git-push", gitApi::stream)
            }
        }

//        resources("/**", ClassPathResource("static/"))
    }.filter { request, next -> next.handle(request) }
}