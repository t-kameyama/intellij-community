// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.vcs.changes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface VcsIgnoreManager {

  boolean isDirectoryVcsIgnored(@NotNull @NlsSafe String dirPath);

  boolean isRunConfigurationVcsIgnored(@NotNull @NlsSafe String configurationName);

  void removeRunConfigurationFromVcsIgnore(@NotNull @NlsSafe String configurationName);

  /**
   * Check if the file could be potentially ignored. The implementation expected to be fast-enough.
   * However, this doesn't mean that the file is ignored in VCS.
   * To check if the file ignored use {@link ChangeListManager#isIgnoredFile(VirtualFile)}
   *
   * @param file to check
   * @return true if the file is potentially ignored
   */
  boolean isPotentiallyIgnoredFile(@NotNull VirtualFile file);

  /**
   * @see #isPotentiallyIgnoredFile(VirtualFile)
   */
  boolean isPotentiallyIgnoredFile(@NotNull FilePath filePath);

  static VcsIgnoreManager getInstance(@NotNull Project project) {
    return project.getService(VcsIgnoreManager.class);
  }
}
