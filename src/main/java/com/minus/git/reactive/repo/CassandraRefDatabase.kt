package com.minus.git.reactive.repo

import com.minus.git.reactive.repo.store.RefStore
import org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.util.RefList
import java.io.IOException

internal class CassandraRefDatabase(repository: DfsRepository) : DfsRefDatabase(repository) {
    private val refs: RefStore

    init {
        refs = RefStore(repository.keyspace)
    }

    @Throws(IOException::class)
    override fun compareAndPut(oldRef: Ref?, newRef: Ref): Boolean {
        val name = newRef.name
        if (oldRef == null || oldRef.storage == Ref.Storage.NEW) return refs.putIfAbsent(name, newRef) == null
        val cur = refs[name]
        return if (cur != null && cur.compareObjId(oldRef)) {
            refs.replace(name, cur, newRef)
        } else {
            false
        }
    }

    @Throws(IOException::class)
    override fun compareAndRemove(oldRef: Ref): Boolean {
        val name = oldRef.name
        val cur = refs[name]
        return if (cur != null && cur.compareObjId(oldRef)) {
            refs.remove(name, cur)
        } else {
            false
        }
    }

    @Throws(IOException::class)
    override fun scanAllRefs(): RefCache {
        val ids = RefList.Builder<Ref?>()
        val sym = RefList.Builder<Ref?>()
        for (ref in refs.values()) {
            if (ref!!.isSymbolic) {
                sym.add(ref)
            }
            ids.add(ref)
        }
        ids.sort()
        sym.sort()
        return RefCache(ids.toRefList(), sym.toRefList())
    }
}