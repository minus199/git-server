package com.minus.git.server.repo

import com.minus.git.server.Utils
import com.minus.git.server.store.RefStore
import org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.util.RefList
import java.io.IOException

internal class CassandraRefDatabase(repository: DfsRepository) : DfsRefDatabase(repository) {
    /**
     * RefStore object provides access to the Cassandra database
     */
    private val refs: RefStore

    /**
     * Compare a reference, and put if it matches.
     *
     * @param oldRef old value to compare to. If the reference is expected
     * to not exist the old value has a storage of
     * Ref.Storage.NEW and an ObjectId value of null.
     * @param newRef new reference to store.
     * @return true if the put was successful; false otherwise.
     * @throws IOException the reference cannot be put due to a system error.
     */
    @Throws(IOException::class)
    override fun compareAndPut(oldRef: Ref?, newRef: Ref): Boolean {
        val name = newRef.name
        if (oldRef == null || oldRef.storage == Ref.Storage.NEW) return refs.putIfAbsent(name, newRef) == null
        val cur = refs[name]
        return if (cur != null && Utils.refsHaveEqualObjectId(cur, oldRef)) {
            refs.replace(name, cur, newRef)
        } else {
            false
        }
    }

    /**
     * Compare a reference, and delete if it matches.
     *
     * @param oldRef the old reference information that was previously read.
     * @return true     if the remove was successful; false otherwise.
     * @throws IOException the reference could not be removed due to a system
     * error.
     */
    @Throws(IOException::class)
    override fun compareAndRemove(oldRef: Ref): Boolean {
        val name = oldRef.name
        val cur = refs[name]
        return if (cur != null && Utils.refsHaveEqualObjectId(cur, oldRef)) {
            refs.remove(name, cur)
        } else {
            false
        }
    }

    /**
     * Read all known references in the repository.
     *
     * @return all current references of the repository.
     * @throws IOException references cannot be accessed.
     */
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

    /**
     * Constructor
     *
     * @param repository a reference to the repository this ref database
     * is associated with.
     */
    init {
        refs = RefStore(repository.description.repositoryName)
    }
}