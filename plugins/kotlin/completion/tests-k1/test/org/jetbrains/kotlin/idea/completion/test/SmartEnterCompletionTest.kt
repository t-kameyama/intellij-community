// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class SmartEnterCompletionTest : KotlinLightCodeInsightFixtureTestCase() {

    override val pluginMode: KotlinPluginMode
        get() = KotlinPluginMode.K1

    fun testBasic() {
        doTest(
            """
            fun foo(p:String) {}

            fun test() {
                val s = ""
                fo<caret>s
            }
            """, """
            fun foo(p:String) {}

            fun test() {
                val s = ""
                foo(s)<caret>
            }
            """
        )
    }

    fun testEncloseCall() {
        doTest(
            """
            fun foo(p:String) {}
            fun bar(p:String): String = p

            fun test() {
                val s = ""
                fo<caret>bar(s)
            }
            """, """
            fun foo(p:String) {}
            fun bar(p:String): String = p

            fun test() {
                val s = ""
                foo(bar(s))<caret>
            }
            """
        )
    }

    fun testFinishOuterCall() {
        doTest(
            """
            fun foo(p:String) {}
            fun bar(p:String): String = p

            fun test() {
                val s = ""
                foo(ba<caret>s)
            }
            """, """
            fun foo(p:String) {}
            fun bar(p:String): String = p

            fun test() {
                val s = ""
                foo(bar(s))<caret>
            }
            """
        )
    }

    fun testIf() {
        doTest(
            """
            fun foo(p:String): Boolean = false

            fun test() {
                val s = ""
                if (fo<caret>s
            }
            """, """
            fun foo(p:String): Boolean = false

            fun test() {
                val s = ""
                if (foo(s)) {
                    <caret>
                }
            }
            """
        )
    }

    fun testTwoLineBinaryExpr() {
        doTest(
            """
            fun foo(p:Int) {}

            fun test() {
                fo<caret>1 +
                        2
            }
            """, """
            fun foo(p:Int) {}

            fun test() {
                foo(
                    1 +
                            2
                )<caret>
            }
            """
        )
    }

    fun doTest(before: String, after: String) {
        with(myFixture) {
            configureByText(KotlinFileType.INSTANCE, before)
            completeBasic()
            type(Lookup.COMPLETE_STATEMENT_SELECT_CHAR)
            checkResult(after)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_LATEST
}
