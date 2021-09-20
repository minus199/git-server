package com.minus.git.reactive.repo

import com.minus.git.reactive.backend.ProtocolService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.RefUpdate
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.regex.Pattern


fun Repository.isEnabledFor(service: ProtocolService): Boolean {
    return if (service.isOverridable) config.get(service.configKey).enabled else service.isEnabled
}

fun Repository.createDefaultBranch(): RefUpdate = refDatabase.newUpdate(Constants.HEAD, false)
    .apply {
        isForceUpdate = true
        link("refs/heads/master")
    }

val DfsRepository.keyspace: String
    get() = "repository_${description.repositoryName}"

fun CassandraGitRepository.branches(): Flux<Ref> = Flux.fromIterable(git.branchList().call())

fun CassandraGitRepository.commits(branch: String): Flux<RevCommit> =
    git.log().add(resolve(branch)).call().let { Flux.fromIterable(it) }

val p = Pattern.compile("^[a-zA-Z0-9-_]+$")
fun String.gitSanitize(): Mono<String> {
    val output = lowercase(Locale.getDefault())
        .trim { it <= ' ' }
        .let {
            val idx = it.indexOf(".git")
            if (idx >= 0) it.substring(0, idx) else it
        }

    return if (p.matcher(output).matches()) {
        Mono.just(output)
    } else {
        throw IllegalArgumentException("Invalid name: $this")
    }
}

suspend fun DfsRepository.walk(branch: String): Flow<ByteArrayOutputStream> {
    val channel = Channel<ByteArrayOutputStream>()

    val revWalk = RevWalk(this)
    val commit = withContext(Dispatchers.IO) { revWalk.runCatching { parseCommit(resolve(branch)) } }
    val treeWalk = TreeWalk(this)
        .apply {
            isRecursive = true
            commit.mapCatching { addTree(commit.getOrThrow()) }
        }

    return sequence {
        ByteArrayOutputStream().use { out ->
            while (treeWalk.next()) {
                val objectId = treeWalk.getObjectId(0)
                open(objectId).copyTo(out)
                yield(out)
                out.reset()
            }
        }
    }.asFlow()


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

