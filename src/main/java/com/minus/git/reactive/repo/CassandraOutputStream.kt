/*
 * A Cassandra backend for JGit
 * Copyright 2015 Ben Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.minus.git.reactive.repo

import com.minus.git.reactive.repo.store.ObjStore
import org.eclipse.jgit.internal.storage.dfs.DfsOutputStream
import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription
import org.eclipse.jgit.internal.storage.pack.PackExt
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Output stream is used to write data into a file in the Cassandra store.
 */
class CassandraOutputStream(
    private val store: ObjStore,
    private val desc: DfsPackDescription,
    private val ext: PackExt
) : DfsOutputStream() {

    private val dst = ByteArrayOutputStream()
    private var data: ByteArray? = null

    @Throws(IOException::class)
    override fun write(buf: ByteArray, off: Int, len: Int) {
        data = null
        dst.write(buf, off, len)
    }

    override fun read(position: Long, buf: ByteBuffer): Int {
        val d = getData()
        val n = Math.min(buf.remaining(), d!!.size - position.toInt())
        if (n == 0) {
            return -1
        }
        buf.put(d, position.toInt(), n)
        return n
    }

    @Throws(IOException::class)
    override fun flush() {
        getData()
        store.writeFile(desc, ext, ByteBuffer.wrap(data))
    }

    @Throws(IOException::class)
    override fun close() {
        flush()
    }

    private fun getData(): ByteArray? {
        if (data == null) {
            data = dst.toByteArray()
        }
        return data
    }
}