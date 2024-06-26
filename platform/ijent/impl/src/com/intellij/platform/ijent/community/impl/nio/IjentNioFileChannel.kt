// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.ijent.community.impl.nio

import com.intellij.platform.ijent.fs.IjentFileInfo
import com.intellij.platform.ijent.fs.IjentFileSystemApi
import com.intellij.platform.ijent.fs.IjentOpenedFile
import com.intellij.platform.ijent.fs.IjentPath
import com.intellij.platform.ijent.fs.IjentPosixFileInfo
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.*
import java.nio.file.FileSystemException
import java.util.concurrent.atomic.AtomicLong

internal class IjentNioFileChannel private constructor(
  private val nioFs: IjentNioFileSystem,
  private val ijentOpenedFile: IjentOpenedFile,
  private val position: AtomicLong,
) : FileChannel() {
  companion object {
    @JvmStatic
    internal suspend fun createReading(nioFs: IjentNioFileSystem, path: IjentPath.Absolute): IjentNioFileChannel =
      IjentNioFileChannel(nioFs, nioFs.ijent.fs.fileReader(path).getOrThrowFileSystemException(), AtomicLong(0))

    @JvmStatic
    internal suspend fun createWriting(
      nioFs: IjentNioFileSystem,
      path: IjentPath.Absolute,
      append: Boolean,
      truncate: Boolean,
      creationMode: IjentFileSystemApi.FileWriterCreationMode,
    ): IjentNioFileChannel {

      return IjentNioFileChannel(
        nioFs,
        nioFs.ijent.fs.fileWriter(path, append = append, truncateExisting = truncate, creationMode = creationMode).getOrThrowFileSystemException(),
        AtomicLong(0)
      )
    }
  }

  override fun read(dst: ByteBuffer): Int {
    when (ijentOpenedFile) {
      is IjentOpenedFile.Reader -> Unit
      is IjentOpenedFile.Writer -> throw NonReadableChannelException()
    }
    val readResult = nioFs.fsBlocking {
        ijentOpenedFile.read(dst)
      }.getOrThrowFileSystemException()
    return when (readResult) {
      is IjentOpenedFile.Reader.ReadResult.Bytes -> readResult.bytesRead
      is IjentOpenedFile.Reader.ReadResult.EOF -> -1
    }
  }

  override fun read(dsts: Array<out ByteBuffer>, offset: Int, length: Int): Long {
    when (ijentOpenedFile) {
      is IjentOpenedFile.Reader -> Unit
      is IjentOpenedFile.Writer -> throw NonReadableChannelException()
    }

    var totalRead = 0L
    nioFs.fsBlocking {
      handleThatSmartMultiBufferApi(dsts, offset, length) { buf ->
        val read = when (val res = ijentOpenedFile.read(buf).getOrThrowFileSystemException()) {
          is IjentOpenedFile.Reader.ReadResult.Bytes -> res.bytesRead
          is IjentOpenedFile.Reader.ReadResult.EOF -> return@fsBlocking
        }
        totalRead += read
      }
    }
    return totalRead
  }

  override fun write(src: ByteBuffer): Int {
    when (ijentOpenedFile) {
      is IjentOpenedFile.Writer -> Unit
      is IjentOpenedFile.Reader -> throw NonWritableChannelException()
    }

    val bytesWritten = nioFs
      .fsBlocking {
        ijentOpenedFile.write(src)
      }
      .getOrThrowFileSystemException()
    position.addAndGet(bytesWritten.toLong())
    return bytesWritten
  }

  override fun write(srcs: Array<out ByteBuffer>, offset: Int, length: Int): Long {
    when (ijentOpenedFile) {
      is IjentOpenedFile.Writer -> Unit
      is IjentOpenedFile.Reader -> throw NonWritableChannelException()
    }

    var totalWritten = 0L
    nioFs.fsBlocking {
      handleThatSmartMultiBufferApi(srcs, offset, length) { buf ->
        val written = ijentOpenedFile.write(buf).getOrThrowFileSystemException()
        if (written <= 0) {  // A non-strict comparison.
          return@fsBlocking
        }
        else {
          totalWritten += written
          position.getAndAdd(written.toLong())
        }
      }
    }
    return totalWritten
  }

  private inline fun handleThatSmartMultiBufferApi(
    buffers: Array<out ByteBuffer>,
    offset: Int,
    length: Int,
    body: (ByteBuffer) -> Unit,
  ) {
    if (buffers.isEmpty()) throw IndexOutOfBoundsException()
    if (offset !in 0..<buffers.size) throw IndexOutOfBoundsException()
    if (length < 0) throw IndexOutOfBoundsException()
    if (length > buffers.size - offset) throw IndexOutOfBoundsException()

    val iter = buffers.asSequence().take(length).iterator()
    if (iter.hasNext()) {
      var buf = iter.next()
      buf.position(buf.position() + offset)
      while (true) {
        body(buf)  // Can return through the whole function.
        buf = when {
          buf.hasRemaining() -> buf
          iter.hasNext() -> iter.next()
          else -> break
        }
      }
    }
  }

  override fun position(): Long {
    return nioFs.fsBlocking {
      ijentOpenedFile.tell().getOrThrowFileSystemException()
    }
  }

  override fun position(newPosition: Long): FileChannel {
    return nioFs.fsBlocking {
      ijentOpenedFile.seek(newPosition, IjentOpenedFile.SeekWhence.START).getOrThrowFileSystemException()
      this@IjentNioFileChannel
    }
  }

  override fun size(): Long {
    return nioFs.fsBlocking {
      return@fsBlocking when (val type = nioFs.ijent.fs.stat(ijentOpenedFile.path, false).getOrThrowFileSystemException().type) {
        is IjentFileInfo.Type.Regular -> type.size
        is IjentFileInfo.Type.Directory, is IjentFileInfo.Type.Other -> throw IOException("This file channel is opened for a directory")
        is IjentPosixFileInfo.Type.Symlink -> throw IllegalStateException("Internal error: symlink should be resolved for a file channel")
      }
    }
  }

  override fun truncate(size: Long): FileChannel = apply {
    TODO("Not yet implemented")
  }

  override fun force(metaData: Boolean) {
    TODO("Not yet implemented")
  }

  override fun transferTo(position: Long, count: Long, target: WritableByteChannel): Long {
    TODO("Not yet implemented")
  }

  override fun transferFrom(src: ReadableByteChannel, position: Long, count: Long): Long {
    TODO("Not yet implemented")
  }

  override fun read(dst: ByteBuffer, position: Long): Int {
    TODO("Not yet implemented")
  }

  override fun write(src: ByteBuffer, position: Long): Int {
    TODO("Not yet implemented")
  }

  override fun map(mode: MapMode, position: Long, size: Long): MappedByteBuffer =
    throw UnsupportedOperationException()

  override fun lock(position: Long, size: Long, shared: Boolean): FileLock {
    TODO("Not yet implemented")
  }

  override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock? {
    TODO("Not yet implemented")
  }

  @Throws(FileSystemException::class)
  override fun implCloseChannel() {
    nioFs.fsBlocking {
      ijentOpenedFile.close()
    }
  }
}