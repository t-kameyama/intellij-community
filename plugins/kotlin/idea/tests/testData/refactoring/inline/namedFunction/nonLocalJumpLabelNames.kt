// COMPILER_ARGUMENTS: -Xnon-local-break-continue
// IGNORE_K1

// function name doesn't count for labels
fun loop4() {
    loop@ for (i in 0..10) {
        loop1@ for (j in 0..10) {
            for (k in 0..10) {
                foo {
                    loop2@ for (l in 0..10) {
                        loop3@ while (true) {
                            break@loop3
                        }
                    }
                    break
                }
            }
        }
    }
}

inline fun foo<caret>(body: () -> Unit) {
    while (true) {
        body()
    }
}
