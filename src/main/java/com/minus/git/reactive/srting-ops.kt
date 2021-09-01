package com.minus.gitengine.backend

import org.eclipse.jgit.lib.ObjectId
import java.util.regex.Pattern

fun String.toObjId(): ObjectId = ObjectId.fromString(this)

private val sanitizePattern = Pattern.compile("^[a-zA-Z0-9-_/]+$")

@Throws(IllegalArgumentException::class)
fun String.sanitiseName(): String {
    val str = lowercase().trim { it <= ' ' }.let {
        val idx = it.indexOf(".git")
        if (idx >= 0) it.substring(0, idx) else it
    }

    if (sanitizePattern.matcher(str).matches()) return str

    throw IllegalArgumentException("Invalid name: $this")
}


fun ByteArray.toHexString(): String {
    return this.joinToString("") {
        String.format("%02x", it)
    }
}