package com.minus.git.reactive.http

import com.minus.git.reactive.http.api.GitApi
import com.minus.git.reactive.http.api.RepositoriesApi
import com.minus.git.reactive.http.api.SSE
import com.minus.git.reactive.service.RepoName
import org.springframework.http.MediaType.ALL
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Component
class Routes(private val gitApi: GitApi, private val repositoriesApi: RepositoriesApi, private val sse: SSE) {
    private fun gitRouter() = router {
        "/{repoName}.git".nest {
            accept(ALL).nest {
                GET("", gitApi::head)
                GET("/HEAD", gitApi::head)
                GET("/info/refs", gitApi::infoRefs)

                POST("/git-upload-pack", gitApi::uploadPack)
                POST("/git-receive-pack", gitApi::receivePack)

                "/objects".nest {
                    GET("/info/alternates", gitApi::textFile)
                    GET("/info/http-alternates", gitApi::textFile)
                    GET("/info/packs", gitApi::infoPacks)

                    GET("/{objGroup:[0-9a-f]\\{2\\}}/{objID: [0-9a-f]\\{38\\}$}", gitApi::looseObject)
                    GET("/pack/{packIdPack:pack-[0-9a-f]\\{40\\}\\.pack$}", gitApi::packFile)
                    GET("/pack/{packIdx:pack-[0-9a-f]{40}\\.idx$}", gitApi::idxFile)
                }
            }
        }
    }

    private fun apiRouter() = router {
        "/api".nest {
            "/repositories".nest {
                GET("") { req ->
                    ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .body(repositoriesApi.allRepos(req), RepoName::class.java)
                }

                "/{repoName}".nest {
                    GET("", repositoriesApi::repoInfo)
                    POST("", repositoriesApi::create)
                    DELETE("", repositoriesApi::delete)
                    GET("/branches", repositoriesApi::branches)

                    "/{branch}".nest {
                        GET("/commits", repositoriesApi::commits)
                    }
                }
            }
        }
    }

    private fun sseRouter() = router {
        "/sse".nest {
            accept(TEXT_EVENT_STREAM).nest {
                GET("/git-push", sse::stream)
            }
        }
    }

    fun rootRouter(): RouterFunction<ServerResponse> = router {
        add(gitRouter())
        add(apiRouter())
        add(sseRouter())
        // resources("/**", ClassPathResource("static/"))
    }.filter { request, next -> next.handle(request) }
}