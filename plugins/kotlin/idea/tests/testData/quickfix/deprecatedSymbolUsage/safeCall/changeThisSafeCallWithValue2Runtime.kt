// "Replace with 's.filter { it != c }'" "true"
// WITH_STDLIB

class X {
    @Deprecated("", ReplaceWith("s.filter { it != c }"))
    fun oldFun(s: String): CharSequence = s.filter { it != c }

    val c = 'x'
}

fun foo(x: X?, s: String) {
    bar(x?.<caret>oldFun(s))
}

fun bar(s: CharSequence?){}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.replaceWith.DeprecatedSymbolUsageFix