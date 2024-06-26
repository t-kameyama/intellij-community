// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.codeInsight;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testIntegration.TestFramework;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TestFrameworksImpl extends TestFrameworks {
  private TestFrameworksImpl() {
  }

  @Override
  public boolean isTestClass(final @NotNull PsiClass psiClass) {
    for (TestFramework framework : DumbService.getDumbAwareExtensions(psiClass.getProject(), TestFramework.EXTENSION_NAME)) {
      if (framework.isTestClass(psiClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isPotentialTestClass(@NotNull PsiClass psiClass) {
    for (TestFramework framework : DumbService.getDumbAwareExtensions(psiClass.getProject(), TestFramework.EXTENSION_NAME)) {
      if (framework.isPotentialTestClass(psiClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable PsiMethod findOrCreateSetUpMethod(final PsiClass psiClass) {
    for (TestFramework framework : DumbService.getDumbAwareExtensions(psiClass.getProject(), TestFramework.EXTENSION_NAME)) {
      if (framework.isTestClass(psiClass)) {
        try {
          final PsiMethod setUpMethod = (PsiMethod)framework.findOrCreateSetUpMethod(psiClass);
          if (setUpMethod != null) {
            return setUpMethod;
          }
        }
        catch (IncorrectOperationException e) {
          //skip
        }
      }
    }
    return null;
  }

  @Override
  public @Nullable PsiMethod findSetUpMethod(final PsiClass psiClass) {
    for (TestFramework framework : DumbService.getDumbAwareExtensions(psiClass.getProject(), TestFramework.EXTENSION_NAME)) {
      if (framework.isTestClass(psiClass)) {
        final PsiMethod setUpMethod = (PsiMethod)framework.findSetUpMethod(psiClass);
        if (setUpMethod != null) {
          return setUpMethod;
        }
      }
    }
    return null;
  }

  @Override
  public @Nullable PsiMethod findTearDownMethod(final PsiClass psiClass) {
    for (TestFramework framework : DumbService.getDumbAwareExtensions(psiClass.getProject(), TestFramework.EXTENSION_NAME)) {
      if (framework.isTestClass(psiClass)) {
        final PsiMethod setUpMethod = (PsiMethod)framework.findTearDownMethod(psiClass);
        if (setUpMethod != null) {
          return setUpMethod;
        }
      }
    }
    return null;
  }

  @Override
  protected boolean hasConfigMethods(PsiClass psiClass) {
    for (TestFramework framework : DumbService.getDumbAwareExtensions(psiClass.getProject(), TestFramework.EXTENSION_NAME)) {
      if (framework.findSetUpMethod(psiClass) != null || framework.findTearDownMethod(psiClass) != null) return true;
    }
    return false;
  }

  @Override
  public boolean isTestMethod(PsiMethod method) {
    for (TestFramework framework : DumbService.getDumbAwareExtensions(method.getProject(), TestFramework.EXTENSION_NAME)) {
      if (framework.isTestMethod(method)) return true;
    }
    return false;
  }

  @Override
  public boolean isTestMethod(PsiMethod method, boolean checkAbstract) {
    for (TestFramework framework : DumbService.getDumbAwareExtensions(method.getProject(), TestFramework.EXTENSION_NAME)) {
      if (framework.isTestMethod(method, checkAbstract)) return true;
    }
    return false;
  }
}