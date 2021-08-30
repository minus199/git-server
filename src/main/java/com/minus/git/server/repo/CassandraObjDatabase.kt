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
package com.minus.git.server.repo

import com.minus.git.server.store.ObjStore
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
    /**
     * ObjStore object provides access to the Cassandra database
     */
    private val objstore: ObjStore

    /**
     * Constructor
     *
     * @param repository a reference to the repository this ref database
     * is associated with.
     * @param conn       connection to the Cassandra data store.
     */
    init {
        objstore = ObjStore(repository.description.repositoryName, repository.description)
    }

    /**
     * Implementation of pack commit.
     *
     * @param desc     description of the new packs.
     * @param replaces if not null, list of packs to remove.
     * @throws IOException if the packs could not be committed
     */
    @Throws(IOException::class)
    override fun commitPackImpl(desc: Collection<DfsPackDescription>, replaces: Collection<DfsPackDescription>?) {
        if (replaces != null && !replaces.isEmpty()) {
            objstore.removeDesc(replaces)
        }

        objstore.insertDesc(desc)
    }

    /**
     * List the available pack files.
     * The returned list supports random access and is mutable by the caller.
     *
     * @return available packs. May be empty if there are no packs.
     * @throws java.io.IOException  if a list of packs could not be retrieved
     * from the store
     */
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
        DfsPackDescription(
            repository.description,
            UUID.randomUUID().toString() + "-" + source.name,
            source
        ).apply { packSource = source }

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