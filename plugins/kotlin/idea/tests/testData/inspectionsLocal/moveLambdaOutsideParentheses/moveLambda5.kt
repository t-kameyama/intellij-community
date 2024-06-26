// IS_APPLICABLE: true
// HIGHLIGHT: INFORMATION

fun foo() {
    bar(name1 = 3, name2 = 2, name3 = 1, name4 = <caret>{ it })
}

fun bar(name1: Int, name2: Int, name3: Int, name4: (Int) -> Int) : Int {
    return name4(name1) + name2 + name3
}
