// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.execution.wsl

import com.intellij.openapi.util.io.FileUtil.toSystemDependentName
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.openapi.vfs.impl.wsl.WslConstants

data class WslPath internal constructor(private val prefix: String = WslConstants.UNC_PREFIX,
                                        val distributionId: String,
                                        val linuxPath: String) {

  constructor(distributionId: String, linuxPath: String) : this(WslConstants.UNC_PREFIX, distributionId, linuxPath)

  init {
    if (!prefix.endsWith("\\")) {
      throw AssertionError("$prefix should end with \\")
    }
  }

  val distribution: WSLDistribution by lazy {
    WslDistributionManager.getInstance().getOrCreateDistributionByMsId(distributionId)
  }

  val wslRoot: String
    get() = prefix + distributionId

  fun toWindowsUncPath(): String = prefix + distributionId + toSystemDependentName(linuxPath)

  companion object {

    /**
     * Parses a Windows UNC path and returns a `WslPath` object if successful.
     * The path should start with `\\wsl.localhost\` or `\\wsl$\` prefix.
     * Both prefixes are supported regardless of the preferred prefix on the particular Windows.
     *
     * @param windowsUncPath The Windows UNC path to be parsed.
     * @return A `WslPath` object if the path is successfully parsed, or `null` otherwise.
     *         On OSes not supporting WSL, the method returns null.
     */
    @JvmStatic
    fun parseWindowsUncPath(windowsUncPath: String): WslPath? {
      if (!WSLUtil.isSystemCompatible()) return null
      val path = toSystemDependentName(windowsUncPath, '\\')

      val legacyWslPrefix = WslConstants.UNC_PREFIX
      val modernWslPrefix = WSLUtil.DEFAULT_UNC_PREFIX

      return parseWindowsUncPath(path, legacyWslPrefix) ?: parseWindowsUncPath(path, modernWslPrefix)
    }

    private fun parseWindowsUncPath(path: String, prefix: String): WslPath? {
      if (path.startsWith(prefix, true)) {
        val slashIndex = path.indexOf('\\', prefix.length)
        if (slashIndex > prefix.length) {
          return WslPath(prefix, path.substring(prefix.length, slashIndex), toSystemIndependentName(path.substring(slashIndex)))
        }
      }
      return null
    }

    @JvmStatic
    fun getDistributionByWindowsUncPath(windowsUncPath: String): WSLDistribution? = parseWindowsUncPath(windowsUncPath)?.distribution

    @JvmStatic
    fun isWslUncPath(windowsUncPath: String): Boolean = parseWindowsUncPath(windowsUncPath) != null
  }
}
