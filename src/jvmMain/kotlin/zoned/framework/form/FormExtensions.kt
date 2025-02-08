package zoned.framework.form

import io.javalin.http.Context
import zoned.framework.form.FormConversionChecker.ConversionReport
import zoned.framework.auth.AuthUser
import zoned.framework.db.FormObject
import zoned.framework.ui.layouts.ResponseContext
import java.util.*
import kotlin.reflect.KClass

inline fun <reified T> Context.entity() = bodyAsClass(T::class.java)

fun <T> AuthUser.withFormObject(entity: T?) = ResponseContext(this, entity)

inline fun <reified T: FormObject> AuthUser.withForm(entity: ConvertedEntity<T>): ResponseContext<T> {
    return ResponseContext(this, entity.entity()).let {
        if (entity.validated != null) {
            it.addValidation(T::class, entity.validated)
        } else {
            it
        }
    }
}

fun <T> ResponseContext<T>.withId(id: UUID?) = this.apply {
    this.id = id
}

fun <T> AuthUser.withNoEntity() = ResponseContext(this, null as T?)

// deprecated
inline fun <reified T : Any> Context.toFormObject(): Pair<T?, ConversionReport> {
    return FormConverter().toFormObject(formParamMap(), T::class)
}

inline fun <reified T : FormObject> Context.parseForm(): ConvertedEntity<T> {
    return FormConverter().parseForm(formParamMap(), T::class)
}

fun <T : FormObject> Context.parseForm(clzz: KClass<T>): ConvertedEntity<T> {
    return FormConverter().parseForm(formParamMap(), clzz)
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class FormList(val type: KClass<*>)

/**
 * XXX This has to be listed with multiple use sites for now
 */
// TODO get rid of this
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class FormName(val name: String)