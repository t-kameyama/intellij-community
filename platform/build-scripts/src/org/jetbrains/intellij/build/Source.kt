// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import org.jetbrains.intellij.build.impl.projectStructureMapping.DistributionFileEntry
import org.jetbrains.jps.model.serialization.JpsMavenSettings.getMavenRepositoryPath
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher

const val UTIL_JAR: String = "util.jar"
const val PLATFORM_LOADER_JAR: String = "platform-loader.jar"
const val UTIL_RT_JAR: String = "util_rt.jar"
const val UTIL_8_JAR: String = "util-8.jar"

internal val isWindows: Boolean = System.getProperty("os.name").startsWith("windows", ignoreCase = true)

private val USER_HOME = Path.of(System.getProperty("user.home"))
internal val MAVEN_REPO: Path = Path.of(getMavenRepositoryPath())

sealed interface Source {
  val filter: ((String) -> Boolean)?
    get() = null
}

class LazySource(
  @JvmField internal val name: String,
  @JvmField val precomputedHash: Long,
  private val sourceSupplier: suspend () -> Sequence<Source>,
) : Source {
  suspend fun getSources(): Sequence<Source> = sourceSupplier()

  override fun toString(): String = "LazySource(name=$name, precomputedHash=$precomputedHash)"
}

data class UnpackedZipSource(
  @JvmField val file: Path,
  override val filter: ((String) -> Boolean)? = null,
) : Source {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ZipSource) return false

    if (file != other.file) return false
    if (filter != other.filter) return false

    return true
  }

  override fun hashCode(): Int {
    var result = file.hashCode()
    result = 31 * result + filter.hashCode()
    return result
  }
}

class CustomAssetShimSource(
  @JvmField val task: suspend (pluginDir: Path, context: BuildContext) -> List<DistributionFileEntry>,
) : Source

data class ZipSource(
  @JvmField val file: Path,
  @JvmField val isPreSignedAndExtractedCandidate: Boolean = false,
  @JvmField val optimizeConfigId: String? = null,
  @JvmField val distributionFileEntryProducer: DistributionFileEntryProducer?,
  override val filter: ((String) -> Boolean),
) : Source, Comparable<ZipSource> {
  init {
    assert(Files.isRegularFile(file)) { "'$file' is not a file" }
  }

  override fun compareTo(other: ZipSource): Int {
    return if (isWindows) file.toString().compareTo(other.file.toString()) else file.compareTo(other.file)
  }

  override fun toString(): String {
    val shortPath = when {
      file.startsWith(MAVEN_REPO) -> MAVEN_REPO.relativize(file).toString()
      file.startsWith(USER_HOME) -> "~/" + USER_HOME.relativize(file)
      else -> file.toString()
    }
    return "zip(file=$shortPath)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ZipSource) return false

    if (file != other.file) return false
    if (isPreSignedAndExtractedCandidate != other.isPreSignedAndExtractedCandidate) return false
    if (filter != other.filter) return false

    return true
  }

  override fun hashCode(): Int {
    var result = file.hashCode()
    result = 31 * result + isPreSignedAndExtractedCandidate.hashCode()
    result = 31 * result + filter.hashCode()
    return result
  }
}

data class DirSource(
  @JvmField val dir: Path,
  @JvmField val excludes: List<PathMatcher> = emptyList(),
  @JvmField val prefix: String = "",
) : Source {
  init {
    assert(!Files.isRegularFile(dir)) { "'$dir' should not be a file" }
  }

  override fun toString(): String {
    val shortPath = if (dir.startsWith(USER_HOME)) "~/${USER_HOME.relativize(dir)}" else dir.toString()
    return "dir(dir=$shortPath, excludes=${excludes.size})"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DirSource) return false

    if (dir != other.dir) return false
    if (excludes != other.excludes) return false
    if (prefix != other.prefix) return false

    return true
  }

  override fun hashCode(): Int {
    var result = dir.hashCode()
    result = 31 * result + excludes.hashCode()
    result = 31 * result + prefix.hashCode()
    return result
  }
}

data class InMemoryContentSource(@JvmField val relativePath: String, @JvmField val data: ByteArray) : Source {
  override fun toString(): String = "InMemory(relativePath=$relativePath)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is InMemoryContentSource) return false

    if (relativePath != other.relativePath) return false
    if (!data.contentEquals(other.data)) return false
    return true
  }

  override fun hashCode(): Int {
    var result = relativePath.hashCode()
    result = 31 * result + data.contentHashCode()
    return result
  }
}
