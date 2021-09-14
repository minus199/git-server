package com.minus.git.reactive.repo

//import com.minus.gitengine.backend.RefRepository
import com.minus.git.reactive.toObjId
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectIdRef
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.SymbolicRef
import java.io.IOException


/**
 * Compares references by object id.
 *
 * @return true if the refs a & b have the same object id, also true if
 * the object ids for both refs are null, otherwise false
 */
fun Ref.compareObjId(other: Ref?): Boolean =
    if (other == null) false
    else if (objectId == null && other.objectId == null) {
        true
    } else if (objectId != null) {
        objectId.equals(other.objectId)
    } else false

fun Ref.isNew() = storage === Ref.Storage.NEW

@Throws(IOException::class)
fun Ref.toModel(repoName: String): RefModel {
    val normalizedObjectId = objectId ?: ObjectId.zeroId()

    return when (this) {
        is SymbolicRef -> RefModel(null, name, repoName, RefType.SYMBOLIC, target.objectId, "")
        is ObjectIdRef.PeeledNonTag -> RefModel(
            null,
            name,
            repoName,
            RefType.PEELED_NONTAG,
            normalizedObjectId,
            ""
        )
        is ObjectIdRef.PeeledTag -> RefModel(
            null,
            name,
            repoName,
            RefType.PEELED_TAG,
            normalizedObjectId,
            peeledObjectId.toString()
        )
        is ObjectIdRef.Unpeeled -> RefModel(null, name, repoName, RefType.UNPEELED, normalizedObjectId, "")
        else -> throw IllegalStateException("Unhandled ref type: $this")
    }
}

//    fun RefModel.save(): Mono<RefModel> = refRepository.save(this)

fun RefModel.toRef(): Ref = when (type) {
    RefType.PEELED_NONTAG -> {
        ObjectIdRef.PeeledNonTag(Ref.Storage.NETWORK, name, value)
    }
    RefType.PEELED_TAG -> {
        ObjectIdRef.PeeledTag(Ref.Storage.NETWORK, name, value, auxValue.toObjId())
    }
    RefType.UNPEELED -> {
        ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, name, value)
    }
    RefType.SYMBOLIC -> {
        throw UnsupportedOperationException()
//            refRepository.findById(value)
//                .map { it.toRef() }
//                .map { SymbolicRef(it.name, it) }
//                .block()!!
    }
}

