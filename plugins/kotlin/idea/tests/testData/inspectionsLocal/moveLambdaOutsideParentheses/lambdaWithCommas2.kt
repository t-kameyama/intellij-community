// IS_APPLICABLE: true
fun bar(x: Int, f: () -> Unit) {}
fun foo(a: Int, b: Int) = 2

fun test() {
    bar(1, {<caret>
        val a = foo(1, 2)
    })
}