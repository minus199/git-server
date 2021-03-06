package com.minus.git.reactive.service

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BatchStatement
import com.datastax.oss.driver.api.core.cql.BatchType
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.Statement
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.minus.git.reactive.repo.store.Tables

interface DatabaseSessionOps {
    //        val loader = DriverConfigLoader.fromClasspath("application.conf")

    val keyspace: String

    private fun session(): CqlSession = CqlSession.builder()
//            .withConfigLoader(loader)
        .build()


    fun Tables.select() = QueryBuilder.selectFrom(keyspace, tableName)
    fun Tables.insert() = QueryBuilder.insertInto(keyspace, tableName)
    fun Tables.delete() = QueryBuilder.deleteFrom(keyspace, tableName)

    fun Statement<*>.execute() = session().use {
        try {
            it.execute(this)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun <T> Statement<*>.execute(mapping: (Row) -> T) = execute().map(mapping)

    fun String.execute(): ResultSet = session().use { it.execute(this) }

    fun String.asStatement(): SimpleStatement = SimpleStatement.newInstance(this)

    fun Array<String>.batch(batchType: BatchType = BatchType.LOGGED) =
        fold(BatchStatement.builder(batchType)) { builder, rawQuery ->
            builder.addStatement(rawQuery.asStatement())
        }.build()
}