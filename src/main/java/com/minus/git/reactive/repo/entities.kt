package com.minus.git.reactive.repo

import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase
import org.eclipse.jgit.lib.ObjectId


class PackDataModel(
    var id: String?,
    var name: String,
    var repository: String,
    var blob: ByteArray
)


class PackDescriptionModel(
    var id: String?,
    var name: String,
    var source: DfsObjDatabase.PackSource, //was was Int //was [var packSrc: DfsObjDatabase.PackSource,]
    var repository: String,
    var indexVersion: Int,
    var objectCount: Long,
    var deltaCount: Long,
    var sizeMap: Map<String, Long>,
    var extensions: Int,
    var lastModified: Long
)

class RefModel(
    var id: String?,
    var name: String,
    var repository: String,
    var type: RefType,
    var value: ObjectId,
    var auxValue: String
)


/**
 * Maps reference types to ints.
 * Used for storage of the reference type (encoded as an int) in the refs
 * table.
 */
enum class RefType(val value: Int) {
    SYMBOLIC(1),
    PEELED_NONTAG(2),
    PEELED_TAG(3),
    UNPEELED(4)
}
