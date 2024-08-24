package zoned.framework.form

import zoned.framework.db.FormObject
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

inline fun <reified T : FormObject> T?.validate(): FormValidation<T> = validate(T::class)

fun <T: FormObject> T?.validate(clzz: KClass<T>): FormValidation<T> {

    val validated = clzz.memberProperties.map { property ->
        val notEmpty = property.findAnnotation<NotEmpty>()
        property to if (notEmpty != null) {
            if (this != null) {
                val propertyVal = property.get(this)
                if (propertyVal !is Collection<*>) {
                    if (propertyVal == null || propertyVal == "") {
                        Validation(false, "Please enter a value for ${notEmpty.label}")
                    } else {
                        // not null, so we're good
                        null
                    }
                } else {
                    // we'll handle below
                    null
                }
            } else {
                // we don't know what type it was, but it shouldn't have been empty
                Validation(false, "${notEmpty.label} is required")
            }
        } else {
            // no validation required
            null
        }
    }.filter {
        it.second != null
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