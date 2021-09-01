package com.minus.git.reactive.repo

import org.eclipse.jgit.internal.storage.dfs.ReadableChannel
import java.io.IOException
import java.nio.ByteBuffer

class CassandraReadableChannel(bytes: ByteBuffer?) : ReadableChannel {
    private val data: ByteArray
    private var position = 0
    private var open = true

    init {
        data = ByteArray(bytes!!.remaining())
        bytes[data]
    }

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer): Int {
        val n = Math.min(dst.remaining(), data.size - position)
        if (n == 0) {
            return -1
        }
        dst.put(data, position, n)
        position += n
        return n
    }

    override fun isOpen(): Boolean = open

    override fun blockSize(): Int = 0

    override fun setReadAheadBytes(p0: Int) {}

    @Throws(IOException::class)
    override fun position(): Long = position.toLong()

    @Throws(IOException::class)
    override fun position(newPosition: Long) {
        position = newPosition.toInt()
    }

    @Throws(IOException::class)
    override fun size(): Long = data.size.toLong()

    @Throws(IOException::class)
    override fun close() {
        open = false
    }
}