// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.test

enum class TargetBackend(
    val isIR: Boolean,
    private val compatibleWithTargetBackend: TargetBackend? = null
) {
    ANY(false),
    JVM(false),
    JVM_OLD(false, JVM),
    JVM_IR(true, JVM),
    JVM_MULTI_MODULE_IR_AGAINST_OLD(true, JVM_IR),
    JVM_MULTI_MODULE_OLD_AGAINST_IR(false, JVM),
    JS(false),
    JS_IR(true, JS),
    JS_IR_ES6(true, JS_IR),
    WASM(true),
    ANDROID(false, JVM),
    JVM_WITH_OLD_EVALUATOR(false),
    JVM_IR_WITH_OLD_EVALUATOR(true),
    JVM_WITH_IR_EVALUATOR(false),
    JVM_IR_WITH_IR_EVALUATOR(true);

    val compatibleWith get() = compatibleWithTargetBackend ?: ANY
}
