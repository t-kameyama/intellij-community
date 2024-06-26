// "Replace with 'newFun(option1, option2, option3, null)'" "true"

interface I {
    @Deprecated("", ReplaceWith("newFun(option1, option2, option3, null)"))
    fun oldFun(option1: String = "", option2: Int = 0, option3: Int = -1)

    fun newFun(option1: String = "", option2: Int = 0, option3: Int = -1, option4: String? = "x")
}

fun foo(i: I) {
    i.<caret>oldFun(option2 = 1)
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.replaceWith.DeprecatedSymbolUsageFix