package zoned.framework.db

import io.javalin.http.Context
import zoned.framework.ui.layouts.ResponseContext

interface FormObject

/**
 * These can be stateful. I.e. a new one is created on every request. (Useful e.g. for validations)
 */
interface FormObjectFactory<T: Entity, U: FormObject> {

    /**
     * Create a new form object, initialized from a db entity
     */
    fun fromEntity(entity: T): U

    /**
     * Final pass before db operations to allow setting of defaults
     * // TODO this could be replaced by logic in FormExtensions + annotations = the main need for this is booleans
     */
    fun toSave(formObject: U): U

    /**
     * If form params indicate that the object is being modified (e.g. child objects added), make the required changes
     */
    fun modifyWithForm(context: Context, formObject: U): U

    /**
     * Return true if modifyWithForm should be called, e.g. if child objects are being added
     */
    fun requiresModification(context: Context): Boolean

    /**
     * Test if the form object is valid, i.e. can be persisted to the db
     */
    fun isValid(formObject: U?): Boolean

    /**
     * Attach any needed validations to the response before sending to render
     */
    fun putValidations(response: ResponseContext<U>): ResponseContext<U>
}