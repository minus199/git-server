package com.minus.git.reactive.http.api

object Patterns {
    val fetch = """^([0-9]{4})([a-z0-9]*)\s([a-z0-9]*)$""".toRegex()
    val eol = """(0{6})([0-9)]{0,2})([a-z]*)""".toRegex()
}

fun String.pktLine() = (length + 4 + 1)
    .toString(16)
    .padStart(4, '0')
    .let { hex -> "$hex$this\n" }

fun String.pktFlush() = "${this}0000"

fun ByteArray.receivePackLine() {
    val len = String(sliceArray(0..3)).toInt(16)
    String(sliceArray(4..len)) // pkt line
    String(sliceArray(len..len + 3)) // magic marker
    String(sliceArray(len + 4..len + 7)) // PACK
    sliceArray(len + 8 until size)  // pack bytes
}
