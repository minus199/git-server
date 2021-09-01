package com.minus.git.reactive.repo.store

import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import com.minus.git.reactive.repo.compareObjId
import com.minus.git.reactive.service.DatabaseSessionOps
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectIdRef.PeeledNonTag
import org.eclipse.jgit.lib.ObjectIdRef.PeeledTag
import org.eclipse.jgit.lib.ObjectIdRef.Unpeeled
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.SymbolicRef
import java.io.IOException

enum class RefType(val value: Int) {
    SYMBOLIC(1), PEELED_NONTAG(2), PEELED_TAG(3), UNPEELED(4);
}

class RefStore(private val keyspace: String) : DatabaseSessionOps {
    @Throws(IOException::class)
    operator fun get(name: String): Ref? {
        val results = selectFrom(keyspace, Tables.PACK_DESC.dbName)
            .all()
            .whereColumn("name")
            .isEqualTo(literal(name))
            .build()
            .execute()

        return rowToRef(results.one()).apply {
            check(results.isFullyFetched) { ("Multiple rows for a single ref: $name") }
        }
    }

    @Throws(IOException::class)
    fun values(): Collection<Ref?> = selectFrom(keyspace, Tables.REFS.dbName)
        .all()
        .build()
        .execute()
        .map { row -> rowToRef(row) }
        .toList()

    @Throws(IOException::class)
    fun putIfAbsent(name: String, newRef: Ref): Ref? {
        val cur = get(name)
        if (cur == null) {
            putRef(name, newRef)
        }
        return cur
    }

    @Throws(IOException::class)
    fun replace(name: String, cur: Ref, newRef: Ref): Boolean {
        val curInStore = get(name)
        return if (curInStore != null && curInStore.compareObjId(cur)) {
            putRef(name, newRef)
            true
        } else {
            false
        }
    }

    @Throws(IOException::class)
    fun remove(name: String, cur: Ref): Boolean {
        val curInStore = get(name)
        return if (curInStore != null && curInStore.compareObjId(cur)) {
            removeRef(name)
            true
        } else {
            false
        }
    }

    @Throws(IOException::class)
    private fun rowToRef(row: Row?): Ref? {
        if (row == null) {
            return null
        }
        val name = row.getString("name")
        val value = row.getString("value")
        return when (val refType = row.getInt("type")) {
            RefType.PEELED_NONTAG.value -> PeeledNonTag(
                Ref.Storage.NETWORK, name,
                ObjectId.fromString(value)
            )
            RefType.PEELED_TAG.value -> PeeledTag(
                Ref.Storage.NETWORK, name,
                ObjectId.fromString(value),
                ObjectId.fromString(row.getString("aux_value"))
            )
            RefType.UNPEELED.value -> Unpeeled(Ref.Storage.NETWORK, name, ObjectId.fromString(value))
            RefType.SYMBOLIC.value -> SymbolicRef(name, get(value!!))
            else -> throw IllegalStateException("Unhandled ref type: $refType")
        }
    }

    @Throws(IOException::class)
    private fun putRef(name: String, r: Ref) {
        when (r) {
            is SymbolicRef -> putRow(name, RefType.SYMBOLIC, r.getTarget().name, "")
            is PeeledNonTag -> putRow(name, RefType.PEELED_NONTAG, r.getObjectId().name(), "")
            is PeeledTag -> putRow(
                name, RefType.PEELED_TAG, r.getObjectId().name(),
                r.getPeeledObjectId().toString()
            )
            is Unpeeled -> putRow(name, RefType.UNPEELED, r.getObjectId().name(), "")
            else -> throw IllegalStateException("Unhandled ref type: $r")
        }
    }

    @Throws(IOException::class)
    private fun putRow(name: String, type: RefType, value: String, auxValue: String) {
        insertInto(keyspace, Tables.REFS.dbName)
            .value("name", literal(name))
            .value("type", literal(type.value))
            .value("value", literal(value))
            .value("aux_value", literal(auxValue))
            .build()
            .execute()
    }

    @Throws(IOException::class)
    private fun removeRef(name: String) {
        deleteFrom(keyspace, Tables.REFS.dbName)
            .whereColumn("name").isEqualTo(literal(name))
            .build()
            .execute()
    }
}