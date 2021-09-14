package com.minus.git.reactive.backend

import org.eclipse.jgit.transport.PacketLineIn
import org.eclipse.jgit.transport.PacketLineOut
import java.io.BufferedInputStream

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PacketCmd(inputStream: BufferedInputStream){
    init{

    }
}
class ReactivePacketLineIn : PacketLineIn {
    constructor(`in`: InputStream) : super(`in`)
    constructor(`in`: InputStream, limit: Long) : super(`in`, limit)
}

class ReactivePacketLineOut
/**
 * Create a new packet line writer.
 *
 * @param outputStream stream.
 */
    (outputStream: OutputStream) : PacketLineOut(outputStream) {

    override fun setFlushOnEnd(flushOnEnd: Boolean) {
        super.setFlushOnEnd(flushOnEnd)
    }

    @Throws(IOException::class)
    override fun writeString(s: String) {
        super.writeString(s)
    }

    @Throws(IOException::class)
    override fun writePacket(packet: ByteArray) {
        super.writePacket(packet)
    }

    @Throws(IOException::class)
    override fun writePacket(buf: ByteArray, pos: Int, len: Int) {
        super.writePacket(buf, pos, len)
    }

    @Throws(IOException::class)
    override fun writeDelim() {
        super.writeDelim()
    }

    @Throws(IOException::class)
    override fun end() {
        super.end()
    }

    @Throws(IOException::class)
    override fun flush() {
        super.flush()
    }
}
