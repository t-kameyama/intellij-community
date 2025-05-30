// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.memberPullUp.PullUpConflictsUtil
import com.intellij.refactoring.memberPullUp.PullUpProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.RefactoringHierarchyUtil
import com.intellij.refactoring.util.classMembers.MemberInfoStorage
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.base.test.InTextDirectivesUtils
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.refactoring.AbstractMemberPullPushTest
import org.jetbrains.kotlin.idea.refactoring.chooseMembers
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering

abstract class AbstractPullUpTest : AbstractMemberPullPushTest() {
    private fun getTargetClassName(file: PsiFile) = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// TARGET_CLASS: ")

    protected fun doKotlinTest(path: String) {
        doTest(path) { file ->
            val targetClassName = getTargetClassName(file)
            val helper = object : KotlinPullUpHandler.TestHelper {
                override fun adjustMembers(members: List<KotlinMemberInfo>) = chooseMembers(members)

                override fun chooseSuperClass(superClasses: List<PsiNamedElement>): PsiNamedElement {
                    if (targetClassName != null) {
                        return superClasses.single { it.qualifiedClassNameForRendering() == targetClassName }
                    }
                    return superClasses.first()
                }
            }
            val dataContext = SimpleDataContext.getSimpleContext(KotlinPullUpHandler.PULL_UP_TEST_HELPER_KEY, helper)
            KotlinPullUpHandler().invoke(project, editor, file, dataContext) 
        }
    }

    // Based on com.intellij.refactoring.PullUpTest.doTest()
    protected fun doJavaTest(path: String) {
        doTest(path) { file ->
            val elementAt = getFile().findElementAt(editor.caretModel.offset)
            val sourceClass = PsiTreeUtil.getParentOfType(elementAt, PsiClass::class.java)!!

            val targetClassName = getTargetClassName(file)
            val superClasses = RefactoringHierarchyUtil.createBasesList(sourceClass, false, true)
            val targetClass = targetClassName?.let { name -> superClasses.first { it.qualifiedName == name } } ?: superClasses.first()

            val storage = MemberInfoStorage(sourceClass) { true }
            val memberInfoList = chooseMembers(storage.getClassMemberInfos(sourceClass))
            val memberInfos = memberInfoList.toTypedArray()

            val targetDirectory = targetClass.containingFile.containingDirectory
            val conflicts = PullUpConflictsUtil.checkConflicts(
                memberInfos,
                sourceClass,
                targetClass,
                targetDirectory.getPackage()!!,
                targetDirectory,
                { psiMethod: PsiMethod -> PullUpProcessor.checkedInterfacesContain(memberInfoList, psiMethod) },
                true
            )
            if (!conflicts.isEmpty) throw BaseRefactoringProcessor.ConflictsInTestsException(conflicts.values())

            PullUpProcessor(sourceClass, targetClass, memberInfos, DocCommentPolicy(DocCommentPolicy.ASIS)).run()
            UIUtil.dispatchAllInvocationEvents()
        }
    }
}
