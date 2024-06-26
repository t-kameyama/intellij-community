// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.codeInsight.inspections.shared

import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.codeinsights.impl.base.KotlinInspectionFacade
import org.jetbrains.kotlin.psi.KtPrefixExpression

class KotlinInspectionFacadeImpl : KotlinInspectionFacade {
    override val simplifyNegatedBinaryExpression: AbstractApplicabilityBasedInspection<KtPrefixExpression>
        get() = SimplifyNegatedBinaryExpressionInspection()
}