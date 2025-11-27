package zoned.framework.form

import zoned.framework.db.FormObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

inline fun <reified T : FormObject> T?.validate(): FormValidation<T> = validate(T::class)

fun <T: FormObject> T?.validate(clzz: KClass<T>): FormValidation<T> {

    val validated = clzz.memberProperties.mapNotNull { property ->
        val validations = mutableListOf<Validation>()

        // Get property value
        val propertyVal = if (this != null) property.get(this) else null

        // Skip collection validation here (handled separately)
        if (propertyVal is Collection<*>) {
            return@mapNotNull null
        }

        // @NotEmpty validation
        property.findAnnotation<NotEmpty>()?.let { notEmpty ->
            if (propertyVal == null || propertyVal == "") {
                validations.add(Validation(false, "Please enter a value for ${notEmpty.label}"))
            }
        }

        // @Email validation
        property.findAnnotation<Email>()?.let { email ->
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
            val value = propertyVal?.toString() ?: ""
            if (value.isNotEmpty() && !emailRegex.matches(value)) {
                validations.add(Validation(false, "${email.label} ${email.message}"))
            }
        }

        // @Pattern validation
        property.findAnnotation<Pattern>()?.let { pattern ->
            val value = propertyVal?.toString() ?: ""
            if (value.isNotEmpty() && !pattern.regex.toRegex().matches(value)) {
                validations.add(Validation(false, "${pattern.label} ${pattern.message}"))
            }
        }

        // @MinLength validation
        property.findAnnotation<MinLength>()?.let { minLen ->
            val value = propertyVal?.toString() ?: ""
            if (value.length < minLen.min) {
                val msg = minLen.message.ifEmpty { "must be at least ${minLen.min} characters" }
                validations.add(Validation(false, "${minLen.label} $msg"))
            }
        }

        // @MaxLength validation
        property.findAnnotation<MaxLength>()?.let { maxLen ->
            val value = propertyVal?.toString() ?: ""
            if (value.length > maxLen.max) {
                val msg = maxLen.message.ifEmpty { "must be at most ${maxLen.max} characters" }
                validations.add(Validation(false, "${maxLen.label} $msg"))
            }
        }

        // @Range validation
        property.findAnnotation<Range>()?.let { range ->
            val numValue = when (propertyVal) {
                is Number -> propertyVal.toDouble()
                is String -> propertyVal.toDoubleOrNull()
                else -> null
            }
            if (numValue != null && (numValue < range.min || numValue > range.max)) {
                val msg = range.message.ifEmpty { "must be between ${range.min} and ${range.max}" }
                validations.add(Validation(false, "${range.label} $msg"))
            }
        }

        // @MatchesField validation
        property.findAnnotation<MatchesField>()?.let { matches ->
            if (this != null) {
                val otherProperty = clzz.memberProperties.find { it.name == matches.fieldName }
                val otherValue = otherProperty?.get(this)
                if (propertyVal != otherValue) {
                    val msg = matches.message.ifEmpty { "must match ${matches.fieldName}" }
                    validations.add(Validation(false, "${matches.label} $msg"))
                }
            }
        }

        // Return first validation error, or null if no errors
        if (validations.isNotEmpty()) {
            property to validations.first()
        } else {
            null
        }
    }.toMap()

    val validatedCollections = clzz.memberProperties.associate { property ->
        if (this != null) {
            val notEmpty = property.findAnnotation<NotEmpty>()
            property to if (notEmpty != null) {
                val propertyVal = property.get(this)
                if (propertyVal is Collection<*>) {
                    propertyVal.map {
                        if (it == null || it == "") {
                            Validation(false, "Please enter a value for ${notEmpty.label}")
                        } else {
                            null
                        }
                    }
                } else {
                    listOf()
                }
            } else {
                listOf()
            }
        } else {
            // handled above
            property to listOf()
        }
    }

    return FormValidation(validated, validatedCollections)
}

inline fun <reified T : FormObject> List<T?>.validate(): List<FormValidation<T>> {
    return map {
        it.validate()
    }
}

fun List<FormValidation<out FormObject>>.valid() = this.all { it.valid() }