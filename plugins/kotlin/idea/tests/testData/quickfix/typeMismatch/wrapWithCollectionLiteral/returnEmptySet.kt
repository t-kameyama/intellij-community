// "Replace with 'emptySet()' call" "true"
// WITH_STDLIB

fun foo(a: String?): Set<String> {
    val w = a ?: return null<caret>
    return setOf(w)
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.WrapWithCollectionLiteralCallFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.WrapWithCollectionLiteralCallFix