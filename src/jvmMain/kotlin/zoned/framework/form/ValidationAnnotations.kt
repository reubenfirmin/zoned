package zoned.framework.form

/**
 * Validates that the annotated field is a valid email address
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class Email(val label: String = "Email", val message: String = "must be a valid email address")

/**
 * Validates that the annotated field matches the given regex pattern
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class Pattern(val regex: String, val label: String = "Field", val message: String = "has invalid format")

/**
 * Validates that the annotated string field has at least the specified length
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class MinLength(val min: Int, val label: String = "Field", val message: String = "")

/**
 * Validates that the annotated string field has at most the specified length
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class MaxLength(val max: Int, val label: String = "Field", val message: String = "")

/**
 * Validates that the annotated numeric field is within the specified range
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class Range(val min: Double, val max: Double, val label: String = "Field", val message: String = "")

/**
 * Validates that the annotated field matches the value of another field
 * Useful for password confirmation, email confirmation, etc.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class MatchesField(val fieldName: String, val label: String = "Field", val message: String = "")
