package com.minus.git.server.store

import com.minus.git.server.SessionOps
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.internal.storage.pack.PackExt
import java.io.IOException
import java.nio.ByteBuffer

class ObjStore(private val keyspace: String, private val repoDesc: DfsRepositoryDescription) : SessionOps {
    init {
        ensureSchemas()
    }

    @Throws(IOException::class)
    fun insertDesc(desc: Collection<DfsPackDescription>) {
        desc.forEach { packDescription ->
            insertInto(keyspace, DESC_TABLE_NAME)
                .value("name", literal(packDescription.toString()))
                .value("source", literal(packDescription.packSource.ordinal))
                .value("last_modified", literal(packDescription.lastModified))
                .value("size_map", literal(packDescription.computeFileSizeMap()))
                .value("object_count", literal(packDescription.objectCount))
                .value("delta_count", literal(packDescription.deltaCount))
                .value("extensions", literal(packDescription.getExtBits()))
                .value("index_version", literal(packDescription.indexVersion))
                .build()
                .execute()
        }
    }

    /**
     * Removes the list of pack descriptions from the store.
     * If one of the descriptions is not present in the store it will be silently ignored.
     *
     * A pack description is removed based on matching the "name" field;
     *  ie the string returned by DfsPackDescrption.toString()
     *
     * @param desc
     * @throws IOException  if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    fun removeDesc(desc: Collection<DfsPackDescription>) {
        for (pd in desc) {
            deleteFrom(keyspace, DESC_TABLE_NAME)
                .whereColumn("name").isEqualTo(literal(pd.toString()))
                .build()
                .execute()
        }
    }

    @Throws(IOException::class)
    fun listPacks(): List<DfsPackDescription> = selectFrom(keyspace, DESC_TABLE_NAME)
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
     *
     * @throws IOException  if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    fun readFile(desc: DfsPackDescription, ext: PackExt?): ByteBuffer {
        val results = selectFrom(keyspace, DATA_TABLE_NAME)
            .all()
            .whereColumn("name")
            .isEqualTo(literal(desc.getFileName(ext)))
            .build()
            .execute()

        return results.one().let{
            check(results.isFullyFetched) { ("Multiple rows for a single file: ${desc.getFileName(ext)}") }
            it!!.getByteBuffer("data")!!
        }
//        return r!!.getByteBuffer("data")!!
    }

    /**
     * Overwrites the file given by the pair "desc" and "ext" witht the data in the "data" ByteArray.
     *
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    fun writeFile(desc: DfsPackDescription, ext: PackExt?, data: ByteBuffer?) {
        insertInto(keyspace, DATA_TABLE_NAME)
            .value("name", literal(desc.getFileName(ext)))
            .value("data", literal(data))
            .build()
            .execute()
    }

    /**
     * Creates the Cassandra keyspace and pack tables if it does not already exist.
     *
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    private fun ensureSchemas() {
        "CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};"
            .execute()

        "CREATE TABLE IF NOT EXISTS $keyspace.$DESC_TABLE_NAME (name varchar PRIMARY KEY, source int, last_modified bigint, size_map map<text, bigint>, object_count bigint, delta_count bigint, extensions int, index_version int);"
            .execute()

        "CREATE TABLE IF NOT EXISTS $keyspace.$DATA_TABLE_NAME (name varchar PRIMARY KEY, data blob);"
            .execute()
    }

    companion object {
        /**
         * Cassandra fetch size. This won't limit the size of a query result set, but
         * a large result set will be broken up into multiple fetches if the result set
         * size exceeds FETCH_SIZE
         */
        private const val FETCH_SIZE = 100

        /**
         * Pack description table name
         */
        const val DESC_TABLE_NAME = "pack_desc"

        /**
         * Pack data table name
         */
        private const val DATA_TABLE_NAME = "pack_data"
    }

}
