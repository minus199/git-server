package com.minus.git.server

import com.minus.git.server.repo.CassandraGitRepository
import org.eclipse.jgit.internal.storage.dfs.DfsRepository
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevWalk
import java.util.Locale
import java.util.regex.Pattern

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



fun DfsRepository.walk() {
//        val repository = CassandraGitRepository(DfsRepositoryDescription("foo3"))
    val revWalk = RevWalk(this)
    val commit = revWalk.parseCommit(resolve(Constants.MASTER))


    val tree = commit.tree
    println(tree)

    val treeWalk = org.eclipse.jgit.treewalk.TreeWalk(this)
    treeWalk.addTree(tree)

    treeWalk.isRecursive = true

    while (treeWalk.next()) {
        open(treeWalk.getObjectId(0)).copyTo(System.out)
    }
//        val objectId = treeWalk.getObjectId(0)
//        val loader = repository.open(objectId)
//        loader.copyTo(System.out);

    revWalk.dispose()
}