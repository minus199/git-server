package com.minus.git.reactive.repo

import com.minus.git.reactive.backend.ProtocolService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.regex.Pattern

fun Repository.isEnabledFor(service: ProtocolService): Boolean {
    return if (service.isOverridable) config.get(service.configKey).enabled else service.isEnabled
}

/** Sanitize repo name */
fun String.sanitize(): String {
    var str = lowercase(Locale.getDefault()).trim { it <= ' ' }
    val idx = str.indexOf(".git")
    str = if (idx >= 0) str.substring(0, idx) else str
    val p = Pattern.compile("^[a-zA-Z0-9-_]+$")
    val m = p.matcher(str)
    return if (m.matches()) {
        str
    } else {
        throw IllegalArgumentException("Invalid name: $this")
    }
}

suspend fun DfsRepository.walk(): Sequence<ByteArrayOutputStream> {
    val channel = Channel<ByteArrayOutputStream>()

//        val repository = CassandraGitRepository(DfsRepositoryDescription("foo3"))
    val revWalk = RevWalk(this)
    val commit = withContext(Dispatchers.IO) { revWalk.runCatching { parseCommit(resolve(Constants.MASTER)) } }
    val treeWalk = TreeWalk(this)
        .apply {
            isRecursive = true
            commit.mapCatching { addTree(commit.getOrThrow()) }
        }

    return sequence<ByteArrayOutputStream> {
        while (treeWalk.next()) {
            val out = ByteArrayOutputStream()
            val objectId = treeWalk.getObjectId(0)

            val x: ObjectLoader = open(objectId)
            objectId.copyTo(out)

            yield(out)
        }
    }


//    val commit = revWalk.parseCommit(resolve(Constants.MASTER))
//
//
//    val tree = commit.tree
//    println(tree)
//
//    val treeWalk = org.eclipse.jgit.treewalk.TreeWalk(this)
//    treeWalk.addTree(tree)
//
//    treeWalk.isRecursive = true
//
//    while (treeWalk.next()) {
//        open(treeWalk.getObjectId(0)).copyTo(System.out)
//    }
//        val objectId = treeWalk.getObjectId(0)
//        val loader = repository.open(objectId)
//        loader.copyTo(System.out);

}


val DfsRepository.keyspace: String
    get() = "repository_${description.repositoryName}"
