package zoned.framework.ui.layouts

import zoned.framework.auth.AuthUser
import zoned.framework.db.FormObject
import zoned.framework.form.FormValidation
import zoned.framework.i18n.I18N
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

// TODO rename to ValidationContext
// TODO remove authUser
data class ResponseContext<T>(val authUser: AuthUser, val entity: T?) {

    private var bundle: I18N.Bundle? = null
    private val validations = mutableMapOf<KClass<*>, FormValidation<*>>()
    private val collectedValidations = mutableMapOf<KClass<*>, List<FormValidation<*>?>>()

    fun <U: FormObject> addValidation(clzz: KClass<U>, validation: FormValidation<U>?): ResponseContext<T> {
        if (validation != null) {
            validations[clzz] = validation
        }
        return this
    }

    fun <U: FormObject> addValidation(clzz: KClass<U>, validation: List<FormValidation<U>?>): ResponseContext<T> {
        collectedValidations[clzz] = validation
        return this
    }

    fun addBundle(bundle: I18N.Bundle): ResponseContext<T> {
        this.bundle = bundle
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <U: FormObject> validation(clzz: KClass<out U>) = validations[clzz] as? FormValidation<U>

    @Suppress("UNCHECKED_CAST")
    fun <U: FormObject> validation(clzz: KClass<out U>, idx: Int) = collectedValidations[clzz]?.get(idx) as? FormValidation<U>?

    var id: UUID? = null

    fun str(key: String) = bundle?.str(key) ?: throw Exception("No bundle configured")

    fun defaultValue(prop: KProperty1<T, *>) =
        if (entity != null) { prop.get(entity)?.toString() } else { null }
}
