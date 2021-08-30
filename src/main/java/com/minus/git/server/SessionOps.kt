package com.minus.git.server

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement

interface SessionOps {
    //        val loader = DriverConfigLoader.fromClasspath("application.conf")


    private fun session(): CqlSession = CqlSession.builder()
//            .withConfigLoader(loader)
        .build()


    fun SimpleStatement.execute() = session().use {
        try{
            it.execute(this)
        }catch (e: Exception){
            e.printStackTrace()
            throw e
        }
    }

    fun String.execute() = session().use { it.execute(this) }
}