package com.minus.git.reactive.repo.store

import com.datastax.oss.driver.api.core.cql.Row
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase
import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.internal.storage.pack.PackExt

/**
 * Provides utility functions for mapping between the DfsPackDescrption
 * object and the data storage layer.
 */

/**
 * Extracts a file size map from the pack description.
 *
 * @param desc
 * @return a map mapping extension strings to file sizes
 */
fun DfsPackDescription.computeFileSizeMap(): Map<String, Long> =
    PackExt.values()
        .fold(HashMap<String, Long>()) { acc, ext ->
            getFileSize(ext).let {
                if (it > 0) acc[ext.extension] = it
            }

            acc
        }

/**
 * Given a map of extension/size, add each to the pack description.
 *
 * @param desc      pack description to mutate
 * @param extsize   a map from extension string to file size
 */
fun DfsPackDescription.setFileSizeMap(extsize: Map<String, Long>) {
    for ((key, value) in extsize) {
        setFileSize(key.lookupExt(), value)
    }
}

/**
 * Given a pack description, return a bit field representing the PackExt's
 * present.
 *
 * The actual mapping from PackExt to bit position is provided by the
 * PackExt class.
 *
 * @param desc  the pack description to query for pack extensions
 * @return  an integer with bits set for each PackExt present in the pack
 * description
 */
fun DfsPackDescription.getExtBits(): Int {
    var bits = 0
    for (ext in PackExt.values()) {
        if (hasFileExt(ext)) {
            bits = bits or ext.bit
        }
    }
    return bits
}

/**
 * Calls addFileExt on the pack description for each PackExt indicated by
 * the bit field "bits"
 *
 * The actual mapping from PackExt to bit position is provided by the
 * PackExt class.
 *
 * @param desc  the pack description to mutate
 * @param bits  the bit field to read from
 */
fun DfsPackDescription.setExtsFromBits(bits: Int) {
    PackExt.values()
        .filter { it.bit and bits != 0 }
        .forEach { addFileExt(it) }
}

/**
 * Converts a row to a DfsPackDescription
 */
fun Row.toPackDescription(repoDesc: DfsRepositoryDescription): DfsPackDescription =
    DfsPackDescription(repoDesc, getString("name"), DfsObjDatabase.PackSource.values()[getInt("source")])
        .apply {
            lastModified = getLong("last_modified")
            objectCount = getLong("object_count")
            deltaCount = getLong("delta_count")
            indexVersion = getInt("index_version")

            setExtsFromBits(getInt("extensions"))
            setFileSizeMap(get("size_map", HashMap::class.java) as HashMap<String, Long>)
        }

/**
 * The PackExt class defines a number of static instances
 */
private fun String.lookupExt(): PackExt {
//            PackExt.values().find { it.extension == extStr } ?: PackExt.newPackExt(extStr)
    for (ext in PackExt.values()) {
        if (ext.extension == this) {
            return ext
        }
    }

    throw Exception("invalid ext: $this")

    // If we get here, the extension does not exist so create it. It gets
    // added to the list of known extensions in PackExt, so next time the
    // lookup will be successful
//        return PackExt.newPackExt(extStr)
}
