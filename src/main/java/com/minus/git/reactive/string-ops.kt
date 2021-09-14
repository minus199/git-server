package com.minus.git.reactive

import org.eclipse.jgit.lib.ObjectId
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

fun InputStream.readUpToChar(stopChar: Char, maxOccurrences: Int): String {
    mark(1000)
    val occurrences = AtomicInteger(0)

    val stringBuilder = StringBuilder()
    var currentChar = this.read().toChar()

    while (occurrences.getAcquire() <= maxOccurrences) {

//    while (currentChar != stopChar) {
        stringBuilder.append(currentChar)
        currentChar = this.read().toChar()
        if (currentChar == stopChar) {
            occurrences.incrementAndGet()
        }

        if (this.available() <= 0) {
            stringBuilder.append(currentChar)
            break
        }
    }
    reset()
    return stringBuilder.toString()
}

fun ByteArray.toHexString(): String {
    return this.joinToString("") {
        String.format("%02x", it)
    }
}

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