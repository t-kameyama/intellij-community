// "Create parameter 'foo'" "false"
// ERROR: Unresolved reference: foo

object A {
    val test: Int get() {
        return <caret>foo
    }
}