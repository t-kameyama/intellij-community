// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.codeInspection.style;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyBundle;
import org.jetbrains.plugins.groovy.codeInspection.BaseInspection;
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor;
import org.jetbrains.plugins.groovy.codeInspection.utils.JavaStylePropertiesUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import static org.jetbrains.plugins.groovy.lang.psi.util.PsiUtilKt.isFake;

public final class JavaStylePropertiesInvocationInspection extends BaseInspection {
  @Override
  protected @NotNull BaseInspectionVisitor buildVisitor() {
    return new BaseInspectionVisitor() {
      @Override
      public void visitMethodCall(@NotNull GrMethodCall methodCall) {
        if (isFake(methodCall)) return;
        if (JavaStylePropertiesUtil.isPropertyAccessor(methodCall)) {
          final String message = GroovyBundle.message("java.style.property.access");
          final GrExpression expression = methodCall.getInvokedExpression();
          if (expression instanceof GrReferenceExpression ref) {
            PsiElement referenceNameElement = ref.getReferenceNameElement();
            registerError(referenceNameElement, message, myFixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
          }
        }
      }
    };
  }

  private static final LocalQuickFix[] myFixes = new LocalQuickFix[]{new JavaStylePropertiesInvocationFixer()};
}
