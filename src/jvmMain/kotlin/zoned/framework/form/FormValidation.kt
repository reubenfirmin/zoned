package zoned.framework.form

import zoned.framework.db.FormObject
import kotlin.reflect.KProperty1

data class FormValidation<T: FormObject>(
    val status: Map<KProperty1<T, *>, Validation?>,
    val listStatus: Map<KProperty1<T, *>, List<Validation?>>) {

    fun validation(prop: KProperty1<*, *>): Validation? {
        return status[prop]
    }

    /**
     * Validate item in a list
     */
    // TODO no idea what the typing should be here
    fun validation(prop: KProperty1<*, *>, idx: Int): Validation? {
        return listStatus[prop]?.getOrNull(idx)
    }

    fun valid() = status.all { it.value?.valid == true } &&
            listStatus.flatMap { it.value }.all { it == null || it.valid }
}

data class Validation(val valid: Boolean, val message: String?)