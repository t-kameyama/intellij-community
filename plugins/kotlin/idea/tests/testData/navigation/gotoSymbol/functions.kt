fun testFunction() {
}

class Some {
  fun testFunInClass() = 12
}

// SEARCH_TEXT: test
// REF: (in Some).testFunInClass()
// REF: testFunction()

