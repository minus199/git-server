package com.minus.git.reactive.repo

import com.minus.git.reactive.repo.store.ObjStore
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsOutputStream
import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription
import org.eclipse.jgit.internal.storage.dfs.DfsReaderOptions
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.internal.storage.dfs.ReadableChannel
import org.eclipse.jgit.internal.storage.pack.PackExt
import java.io.IOException
import java.util.UUID

internal class CassandraObjDatabase(repository: DfsRepository) : DfsObjDatabase(repository, DfsReaderOptions()) {
    private val objstore: ObjStore

    init {
        objstore = ObjStore(repository.keyspace, repository.description)
    }

    @Throws(IOException::class)
    override fun commitPackImpl(desc: Collection<DfsPackDescription>, replaces: Collection<DfsPackDescription>?) {
        if (replaces != null && !replaces.isEmpty()) {
            objstore.removeDesc(replaces)
        }

        objstore.insertDesc(desc)
    }


    @Throws(IOException::class)
    override fun listPacks(): List<DfsPackDescription> = objstore.listPacks()

    /**
     * Generate a new unique name for a pack file.
     *
     * @param source where the pack stream is created
     * @return a unique name for the pack file. Guaranteed not to collide
     * with any other pack file name in the same DFS.
     * @throws IOException if a new pack name could not be generated
     */
    @Throws(IOException::class)
    override fun newPack(source: PackSource): DfsPackDescription =
        DfsPackDescription(repository.description, UUID.randomUUID().toString() + "-" + source.name, source)
    //.apply { packSource = source }

    /**
     * Rollback a pack creation.
     *
     * @param desc pack to delete
     */
    override fun rollbackPack(desc: Collection<DfsPackDescription>) {
        // Since new packs are not persisted until they are committed, no need to do anything here
    }

    /**
     * Open a pack, pack index, or other related file for reading.
     *
     * @param desc description of pack related to the data that will be read.
     * This is an instance previously obtained from listPacks(),
     * but not necessarily from the same DfsObjDatabase instance.
     * @param ext  file extension that will be read i.e "pack" or "idx".
     * @return channel to read the file
     * @throws FileNotFoundException if the specified file does not exist
     * @throws IOException           if the file could not be opened
     */
    @Throws(IOException::class)
    override fun openFile(desc: DfsPackDescription, ext: PackExt): ReadableChannel =
        CassandraReadableChannel(objstore.readFile(desc, ext))

    /**
     * Open a pack, pack index, or other related file for writing.
     *
     * @param desc description of pack related to the data that will be
     * written. This is an instance previously obtained from
     * newPack(PackSource).
     * @param ext  file extension that will be written i.e "pack" or "idx".
     * @return channel to write the file
     * @throws IOException the file cannot be opened.
     */
    @Throws(IOException::class)
    override fun writeFile(desc: DfsPackDescription, ext: PackExt): DfsOutputStream =
        CassandraOutputStream(objstore, desc, ext)
}