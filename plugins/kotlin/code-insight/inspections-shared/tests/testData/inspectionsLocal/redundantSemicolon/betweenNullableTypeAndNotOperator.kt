// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Boolean?
// AFTER_ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Boolean?
// K2_ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'Boolean?'.
// K2_AFTER_ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'Boolean?'.
fun test(a: Any) {
    val b = a as Boolean?;<caret>
    !b
}