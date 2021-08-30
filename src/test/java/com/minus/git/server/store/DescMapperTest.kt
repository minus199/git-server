/*
 * A Cassandra backend for JGit
 * Copyright 2014-2015 Ben Humphreys
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
package com.minus.git.server.store

import org.eclipse.jgit.internal.storage.dfs.DfsPackDescription
import kotlin.Throws
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import com.minus.git.server.store.DescMapperTest
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase
import org.eclipse.jgit.internal.storage.pack.PackExt
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.Exception
import java.util.HashMap

class DescMapperTest {
    private var desc: DfsPackDescription? = null
    @Before
    @Throws(Exception::class)
    fun setUp() {
        desc = DfsPackDescription(DfsRepositoryDescription(), DESC_NAME, DfsObjDatabase.PackSource.COMPACT)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        desc = null
    }

    @Test
    @Throws(Exception::class)
    fun testGetFileSizeMap() {
        // Prepare the desc object
        desc!!.setFileSize(PackExt.PACK, 1L)
        desc!!.setFileSize(PackExt.INDEX, 2L)

        // Test the mapper
        val extSizes = desc!!.computeFileSizeMap()
        Assert.assertEquals(java.lang.Long.valueOf(1), extSizes[PackExt.PACK.extension])
        Assert.assertEquals(java.lang.Long.valueOf(2), extSizes[PackExt.INDEX.extension])
        Assert.assertFalse(extSizes.containsKey(PackExt.BITMAP_INDEX.extension))
    }

    @Test
    @Throws(Exception::class)
    fun testSetFileSizeMap() {
        val extSizes: MutableMap<String, Long> = HashMap()
        extSizes["pack"] = 1L
        extSizes["idx"] = 2L
        desc!!.setFileSizeMap(extSizes)
        Assert.assertEquals(1, desc!!.getFileSize(PackExt.PACK))
        Assert.assertEquals(2, desc!!.getFileSize(PackExt.INDEX))
        Assert.assertEquals(0, desc!!.getFileSize(PackExt.BITMAP_INDEX))
    }

    @Test
    @Throws(Exception::class)
    fun testGetExtBits() {
        // Prepare the desc object
        desc!!.addFileExt(PackExt.PACK)
        desc!!.addFileExt(PackExt.INDEX)
        val bits: Int = desc!!.getExtBits()
        Assert.assertEquals(3, bits.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testSetExtsFromBit() {
        val bits = 3
        desc!!.setExtsFromBits(bits)
        Assert.assertTrue(desc!!.hasFileExt(PackExt.PACK))
        Assert.assertTrue(desc!!.hasFileExt(PackExt.INDEX))
        Assert.assertFalse(desc!!.hasFileExt(PackExt.BITMAP_INDEX))
    }

    companion object {
        private const val DESC_NAME = "testdesc"
    }
}