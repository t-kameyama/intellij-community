// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.formatting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public interface FormattingProgressCallbackFactory {

  static FormattingProgressCallbackFactory getInstance() {
    return ApplicationManager.getApplication().getService(FormattingProgressCallbackFactory.class);
  }

  @Nullable FormattingProgressCallback createProgressCallback(@NotNull Project project, @NotNull PsiFile file, @NotNull Document document);
}
