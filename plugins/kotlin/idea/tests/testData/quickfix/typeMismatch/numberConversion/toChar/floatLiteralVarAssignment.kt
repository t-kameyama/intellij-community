// "Convert expression to 'Char'" "true"
fun test(b: Byte) {
    var char: Char
    char = 1.0f<caret>
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.NumberConversionFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.NumberConversionFix