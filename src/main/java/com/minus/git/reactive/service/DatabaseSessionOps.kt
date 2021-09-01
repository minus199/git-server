package com.minus.git.reactive.service

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BatchStatement
import com.datastax.oss.driver.api.core.cql.BatchType
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.Statement

interface DatabaseSessionOps {
    //        val loader = DriverConfigLoader.fromClasspath("application.conf")


    private fun session(): CqlSession = CqlSession.builder()
//            .withConfigLoader(loader)
        .build()


    fun Statement<*>.execute() = session().use {
        try {
            it.execute(this)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun String.execute(): ResultSet = session().use { it.execute(this) }

    fun String.asStatement(): SimpleStatement = SimpleStatement.newInstance(this)

    fun Array<String>.batch(batchType: BatchType = BatchType.LOGGED) =
        fold(BatchStatement.builder(batchType)) { builder, rawQuery ->
            builder.addStatement(rawQuery.asStatement())
        }.build()
}