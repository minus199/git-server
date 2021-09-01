package com.minus.git.reactive.repo.store

import com.datastax.oss.driver.api.core.cql.BatchStatement
import com.datastax.oss.driver.api.core.cql.BatchType
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import com.minus.git.reactive.service.DatabaseSessionOps
import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.internal.storage.pack.PackExt
import java.io.IOException
import java.nio.ByteBuffer

class ObjStore(private val keyspace: String, private val repoDesc: DfsRepositoryDescription) : DatabaseSessionOps {
    @Throws(IOException::class)
    fun insertDesc(packDescriptions: Collection<DfsPackDescription>) {
        packDescriptions.fold(BatchStatement.builder(BatchType.LOGGED)) { builder, packDescription ->
            insertInto(keyspace, Tables.PACK_DESC.dbName)
                .value("name", literal(packDescription.toString()))
                .value("source", literal(packDescription.packSource.ordinal))
                .value("last_modified", literal(packDescription.lastModified))
                .value("size_map", literal(packDescription.computeFileSizeMap()))
                .value("object_count", literal(packDescription.objectCount))
                .value("delta_count", literal(packDescription.deltaCount))
                .value("extensions", literal(packDescription.getExtBits()))
                .value("index_version", literal(packDescription.indexVersion))
                .build()
                .let { builder.addStatement(it) }
        }.build().execute()
    }

    /**
     * Removes the list of pack descriptions from the store.
     * If one of the descriptions is not present in the store it will be silently ignored.
     *
     * A pack description is removed based on matching the "name" field;
     *  ie the string returned by DfsPackDescrption.toString()
     */
    @Throws(IOException::class)
    fun removeDesc(desc: Collection<DfsPackDescription>) {
        for (pd in desc) {
            deleteFrom(keyspace, Tables.PACK_DESC.dbName)
                .whereColumn("name").isEqualTo(literal(pd.toString()))
                .build()
                .execute()
        }
    }

    @Throws(IOException::class)
    fun listPacks(): List<DfsPackDescription> = selectFrom(keyspace, Tables.PACK_DESC.dbName)
        .all()
        .build()
        .execute()
        .map { row ->
            try {
                row.toPackDescription(repoDesc)
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
        .toList()

    /**
     * Returns a ByteBuffer with the contents of the file given by the pair "desc" and "ext".
     */
    @Throws(IOException::class)
    fun readFile(desc: DfsPackDescription, ext: PackExt?): ByteBuffer {
        val results = selectFrom(keyspace, Tables.PACK_DATA.dbName)
            .all()
            .whereColumn("name")
            .isEqualTo(literal(desc.getFileName(ext)))
            .build()
            .execute()

        return results.one().let {
            check(results.isFullyFetched) { ("Multiple rows for a single file: ${desc.getFileName(ext)}") }
            it!!.getByteBuffer("data")!!
        }
    }

    /**
     * Overwrites the file given by the pair "desc" and "ext" witht the data in the "data" ByteArray.
     */
    @Throws(IOException::class)
    fun writeFile(desc: DfsPackDescription, ext: PackExt?, data: ByteBuffer?) {
        insertInto(keyspace, Tables.PACK_DATA.dbName)
            .value("name", literal(desc.getFileName(ext)))
            .value("data", literal(data))
            .build()
            .execute()
    }


}
