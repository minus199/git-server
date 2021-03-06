package com.minus.git.reactive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.PropertySources
import reactor.tools.agent.ReactorDebugAgent


@PropertySources(
    PropertySource("classpath:application.properties"),
//    PropertySource("classpath:application.api.properties"),
//    PropertySource("classpath:application.ssh.properties"),
//    PropertySource("classpath:application.shell.properties")
)
@SpringBootApplication()
open class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}