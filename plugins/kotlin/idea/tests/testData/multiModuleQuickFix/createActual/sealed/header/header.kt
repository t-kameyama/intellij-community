// "Add missing actual declarations" "true"
// K2_ACTION: "Create actual in 'testModule_JS'" "true"
// K2_TOOL: org.jetbrains.kotlin.idea.k2.codeinsight.inspections.KotlinNoActualForExpectInspection

expect sealed class <caret>Sealed() {
    object Obj : Sealed

    class Klass(x: Int) : Sealed
}