// DISABLE_ERRORS

fun main() {
    foo(listOf(1, 2, 3))
}

fun foo(lst<caret>) {
    println(lst)
}