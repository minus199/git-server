package com.minus.git.reactive.http

import com.minus.git.reactive.service.RepoName
import org.springframework.http.MediaType
import org.springframework.http.MediaType.ALL
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

//fun <T : Any> Flux<T>.ok(): Mono<ServerResponse> = ServerResponse.ok().body(this)


@Component
class Routes(private val pushHandler: GitPushHandler) {
    fun router() = router {
        "/**".nest {
            GET("") { req ->
                val x = req.body { inputMessage, context -> inputMessage.body }.subscribe {
                    println(it) }
                ServerResponse.ok()

                    .contentType(APPLICATION_JSON)
                    .body(pushHandler.allRepos(req), RepoName::class.java)
            }
        }
        "/api".nest {
            "/repositories".nest {
                accept(ALL).nest {
                    GET("") { req ->


                        ServerResponse.ok()
                            .contentType(APPLICATION_JSON)
                            .body(pushHandler.allRepos(req), RepoName::class.java)
//
//                        val x: List<Foo> = arrayOf("a", "b", "c").map { Foo(it, it) }
//                        val z = Flux.fromIterable(x)
//                        ServerResponse.ok().contentType(APPLICATION_JSON)
//                            .body(z, Foo::class.java)

                        /*val x = Flux.range(0, 1000)//.map { "foooo-$it" }.doOnNext{println(it)}

//                            .subscribe { println(it) }
                            .let {
//                                ServerResponse.ok().contentType(APPLICATION_JSON).body(x) }

                            }*/
                    }

                }
            }

//            "/git/{repoName}".nest {
//                accept(ALL).nest {
//                    GET("/HEAD$", pushHandler::head)
//                    GET("/info/refs", pushHandler::infoRefs)
//                    GET("/objects/info/alternates$", pushHandler::textFile)
//                    GET("/objects/info/http-alternates$", pushHandler::textFile)
//                    GET("/objects/info/packs$", pushHandler::infoPacks)
//                    GET("/objects/{objGroup:[0-9a-f]\\{2\\}}/{objID: [0-9a-f]\\{38\\}$}", pushHandler::looseObject)
//                    GET("/objects/pack/{packIdPack:pack-[0-9a-f]\\{40\\}\\.pack$}", pushHandler::packFile)
//                    GET("/objects/pack/{packIdx:pack-[0-9a-f]{40}\\.idx$}", pushHandler::idxFile)
//
//                    POST("/git-upload-pack$", pushHandler::serviceRPC)
//                    POST("/git-receive-pack$", pushHandler::serviceRPC)
//
//                    GET("/git-push/{foo}", pushHandler::findAll)
//                    POST("/git-push/{foo}", pushHandler::findAll)
//                }
//
//                accept(TEXT_EVENT_STREAM).nest {
//                    GET("/git-push", pushHandler::stream)
//                }
//            }
        }

        accept(TEXT_HTML).nest {
            GET("/") { ok().body { outputMessage, context -> outputMessage.setComplete() } }
//            GET("/sse") { ok().render("sse") }
//            GET("/users", userHandler::findAllView)
        }
//        resources("/**", ClassPathResource("static/"))
    }//.filter { request, next -> next.handle(request) }

    companion object {
        val X_GIT_LOOSE_OBJECT = MediaType("application", "x-git-loose-object")
        val X_GIT_PACKED_OBJECTS = MediaType("application", "x-git-packed-objects")
        val X_GIT_PACKED_OBJECTS_TOC = MediaType("application", "x-git-packed-objects-toc")
    }
}