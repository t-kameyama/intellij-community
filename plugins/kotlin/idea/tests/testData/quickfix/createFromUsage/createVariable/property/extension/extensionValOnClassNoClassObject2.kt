// "Create extension property 'A.Companion.foo'" "true"
// K2_AFTER_ERROR: Extension property must have accessors or be abstract.
// K2_AFTER_ERROR: Unresolved reference 'Companion'.
// K2_AFTER_ERROR: Unresolved reference 'foo'.
class A

fun test() {
    val a: Int = A.<caret>foo
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.CreateExtensionCallableFromUsageFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.quickFixes.createFromUsage.K2CreatePropertyFromUsageBuilder$CreatePropertyFromUsageAction