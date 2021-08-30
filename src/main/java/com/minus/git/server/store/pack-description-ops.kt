/*
 * A Cassandra backend for JGit
 * Copyright 2014 Ben Humphreys
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
fun DfsPackDescription.computeFileSizeMap(): Map<String, Long> {
    val sizeMap: MutableMap<String, Long> = HashMap()
    for (ext in PackExt.values()) {
        val sz = getFileSize(ext)
        if (sz > 0) {
            sizeMap[ext.extension] = sz
        }
    }
    return sizeMap
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
    for (ext in PackExt.values()) {
        if (ext.bit and bits != 0) {
            addFileExt(ext)
        }
    }
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
