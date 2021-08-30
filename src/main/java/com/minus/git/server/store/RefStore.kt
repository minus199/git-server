/*
 * A Cassandra backend for JGit
 * Copyright 2014-2015 Ben Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.minus.git.server.store

import com.minus.git.server.SessionOps
import com.minus.git.server.Utils
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectIdRef.PeeledNonTag
import org.eclipse.jgit.lib.ObjectIdRef.PeeledTag
import org.eclipse.jgit.lib.ObjectIdRef.Unpeeled
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.SymbolicRef
import java.io.IOException

/**
 * Provides access to the Ref store.
 *
 *
 * This class provides map (i.e. key/value) semantics, mapping a "name" to
 * a Ref. The map exists within a namespace identified by the "keyspace".
 * Key/value pairs are distinct within a keyspace.
 */
class RefStore(private val keyspace: String) : SessionOps {
    init {
        createSchemaIfNotExist()
    }


    /**
     * Returns the Ref to which the specified name is mapped
     *
     * @param name the name whose associated value is to be returned
     * @return the Ref to which the specified name is mapped, or null if
     * the store contains no mapping for the name
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    operator fun get(name: String): Ref? {
        val results = selectFrom(keyspace, ObjStore.DESC_TABLE_NAME)
            .all()
            .whereColumn("name")
            .isEqualTo(literal(name))
            .build()
            .execute()

        return rowToRef(results.one()).apply {
            check(results.isFullyFetched) { ("Multiple rows for a single ref: $name") }
        }
    }

    /**
     * @return a Collection view of all refs in the store
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    fun values(): Collection<Ref?> = selectFrom(keyspace, TABLE_NAME)
        .all()
        .build()
        .execute()
        .map { row -> rowToRef(row) }
        .toList()

    /**
     * If the specified "name" is not already associated with a "Ref",
     * associate it with the given "Ref".
     *
     * @param name   the name (i.e. key) with which the specified value is
     * to be associated
     * @param newRef the Ref to be associated with the specified name
     * @return the previous Ref associated with the specified name, or null if
     * there was no mapping for the name.
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    fun putIfAbsent(name: String, newRef: Ref): Ref? {
        val cur = get(name)
        if (cur == null) {
            putRef(name, newRef)
        }
        return cur
    }

    /**
     * Replaces the entry for a name only if currently mapped to a given Ref.
     *
     * @param name   name  which the specified value is associated
     * @param cur    Ref expected to be currently associated with the
     * specified key
     * @param newRef Ref to be associated with the specified key
     * @return true if the value was replaced
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    fun replace(name: String, cur: Ref, newRef: Ref): Boolean {
        val curInStore = get(name)
        return if (curInStore != null && Utils.refsHaveEqualObjectId(curInStore, cur)) {
            putRef(name, newRef)
            true
        } else {
            false
        }
    }

    /**
     * Removes the entry for a name only if currently mapped to a given Ref
     *
     * @param name name with which the specified value is associated
     * @param cur  Ref expected to be associated with the specified key
     * @return true if the value was removed
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    fun remove(name: String, cur: Ref): Boolean {
        val curInStore = get(name)
        return if (curInStore != null && Utils.refsHaveEqualObjectId(curInStore, cur)) {
            removeRef(name)
            true
        } else {
            false
        }
    }

    /**
     * Creates the Cassandra keyspace and refs table if it does
     * not already exist.
     *
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    private fun createSchemaIfNotExist() {
        "CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};"
            .execute()

        "CREATE TABLE IF NOT EXISTS $keyspace.$TABLE_NAME (name varchar PRIMARY KEY, type int, value varchar, aux_value varchar);"
            .execute()
    }

    /**
     * Parses a Cassandra refs table row and converts it to a Ref
     *
     * @param row a single Cassandra row to parse
     * @return a ref, or null if the "row" parameter is null
     * @throws IOException           if an exception occurs when communicating to the
     * database
     * @throws IllegalStateException if the "type" field read back from the
     * database is not one of the four handled
     * types (@see RefType).
     */
    @Throws(IOException::class)
    private fun rowToRef(row: Row?): Ref? {
        if (row == null) {
            return null
        }
        val name = row.getString("name")
        val value = row.getString("value")
        val refType = row.getInt("type")
        return if (refType == RefType.PEELED_NONTAG.value) {
            PeeledNonTag(
                Ref.Storage.NETWORK, name,
                ObjectId.fromString(value)
            )
        } else if (refType == RefType.PEELED_TAG.value) {
            val auxValue = row.getString("aux_value")
            PeeledTag(
                Ref.Storage.NETWORK, name,
                ObjectId.fromString(value),
                ObjectId.fromString(auxValue)
            )
        } else if (refType == RefType.UNPEELED.value) {
            Unpeeled(
                Ref.Storage.NETWORK, name,
                ObjectId.fromString(value)
            )
        } else if (refType == RefType.SYMBOLIC.value) {
            SymbolicRef(name, get(value!!))
        } else {
            throw IllegalStateException("Unhandled ref type: $refType")
        }
    }

    /**
     * Inserts a single ref into the database
     *
     * @throws IllegalStateException if the reference concrete type is not
     * one of the four handled classes
     * (@see RefType).
     */
    @Throws(IOException::class)
    private fun putRef(name: String, r: Ref) {
        if (r is SymbolicRef) {
            putRow(name, RefType.SYMBOLIC, r.getTarget().name, "")
        } else if (r is PeeledNonTag) {
            putRow(name, RefType.PEELED_NONTAG, r.getObjectId().name(), "")
        } else if (r is PeeledTag) {
            putRow(
                name, RefType.PEELED_TAG, r.getObjectId().name(),
                r.getPeeledObjectId().toString()
            )
        } else if (r is Unpeeled) {
            putRow(name, RefType.UNPEELED, r.getObjectId().name(), "")
        } else {
            throw IllegalStateException("Unhandled ref type: $r")
        }
    }

    /**
     * Inserts a row into the refs table. This works for both insertion of a
     * new row, and updating an existing row.
     *
     * @param name     the primary key
     * @param type     a type where the value is mapped to an integer through
     * the RefType enum
     * @param value    the value, either a commit id or in the case of a
     * symbolic reference, the target name
     * @param auxValue an additional value, either the peeled object id in the
     * case of a peeled tag ref, or an empty string for all
     * other types of commits
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    private fun putRow(name: String, type: RefType, value: String, auxValue: String) {
        insertInto(keyspace, TABLE_NAME)
            .value("name", literal(name))
            .value("type", literal(type.value))
            .value("value", literal(value))
            .value("aux_value", literal(auxValue))
            .build()
            .execute()
    }

    /**
     * Removes the ref row given by "name".
     *
     *
     * Given name is the primary key (and unique) only a single row will be
     * removed.
     *
     * @throws IOException if an exception occurs when communicating to the
     * database
     */
    @Throws(IOException::class)
    private fun removeRef(name: String) {
        deleteFrom(keyspace, TABLE_NAME)
            .whereColumn("name").isEqualTo(literal(name))
            .build()
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
         * Refs table name
         */
        private const val TABLE_NAME = "refs"
    }

}