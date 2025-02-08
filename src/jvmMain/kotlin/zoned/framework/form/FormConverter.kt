package zoned.framework.form

import zoned.framework.db.FormObject
import zoned.framework.util.toUUID
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*

data class ConvertedEntity<T: FormObject>(val entity: T?,
                                          val converted: FormConversionChecker.ConversionReport,
                                          val validated: FormValidation<T>?) {

    // validated will be non-null if canConvert
    fun valid() = converted.canConvert && validated!!.valid()

    /**
     * Will be non-null if valid() is true
     */
    fun entity() = entity!!
}

class FormConverter {

    private val checker = FormConversionChecker()

    fun <T: FormObject> parseForm(form: Map<String, List<String>>, clzz: KClass<T>): ConvertedEntity<T> {
        val result = toFormObject(form, clzz)
        val validation = if (result.second.canConvert) {
            result.first!!.validate(clzz)
        } else {
            null
        }
        return ConvertedEntity(result.first, result.second, validation)
    }

    // deprecated name
    fun <T : Any> toFormObject(form: Map<String, List<String>>, clzz: KClass<T>): Pair<T?, FormConversionChecker.ConversionReport> {

        // this ensures a unique namespace, and max one level of nesting
        val report = checker.canConvert(clzz)
        if (!report.canConvert) {
            return null to report
        }

        if (form.isEmpty()) {
            return null to FormConversionChecker.ConversionReport(false, listOf("Empty"), listOf())
        }

        val constructor = clzz.primaryConstructor!!

        val argMap = mutableMapOf<String, Any?>()

        // first deal with our happy params (i.e. a single parameter matches a single arg)
        val matchingParams = constructor.parameters.filter {
            it.name in form.keys
        }.map {
            argMap[it.name!!] = convert(it, form[it.name]!!)
            it
        }.toMutableList()

        // TODO if we've matched all form params at this point but the constructor needs more, give a targeted exception

        // now pull together params that are composites of form args
        constructor.parameters.filterNot {
            it in matchingParams
        }.forEach {
            if ((it.type.classifier as KClass<*>) !in checker.simpleTypes) {
                val composite = convertComposite(it, form)
                if (composite != null) {
                    argMap[it.name!!] = composite
                }
                // TODO just check argMap throughout??
                matchingParams.add(it)
            }
        }
        // TODO are generators useful? maybe drop
        // now supply any with generators
        constructor.parameters.filterNot {
            it in matchingParams
        }.forEach {
            val generated = applyGenerator(it)
            if (generated != null) {
                argMap[it.name!!] = generated
            }
            matchingParams.add(it)
        }

        // set any missing booleans to false
        constructor.parameters.filterNot {
            it.name in argMap.keys
        }.filter {
            it.type.classifier == Boolean::class
        }.forEach {
            argMap[it.name!!] = false
        }

        // which params were not resolved?
        val missing = constructor.parameters.filterNot {
            it.name in argMap.keys
        }

        if (missing.filterNot { it.type.isMarkedNullable }.isNotEmpty()) {
            throw CannotConvertException("Could not resolve params ${missing.joinToString(",") { it.name!! }}")
        }

        var allEmpty = true
        val args = constructor.parameters.associateWith {
            val value = argMap[it.name!!]
            allEmpty = allEmpty && (value == null || value.isEmptyCollection())
            value
        }

        return constructor.callBy(args) to report//.withParamsSupplied()
    }

    private fun Any.isEmptyCollection(): Boolean {
        return (this is Collection<*>) && this.isEmpty()
    }

    fun convert(parameter: KParameter, value: List<String>): Any? {

        val nameOverride = parameter.findAnnotation<FormName>()
        if (nameOverride != null) {
            throw Exception("This is no longer supported")
        }

        val type = parameter.type.classifier
        return if (type == List::class || type == Set::class) {
            val typeOfList = parameter.findAnnotation<FormList>()!!.type.createType()
            val collection = value.convertTo(typeOfList, parameter, true)
            if (type == Set::class) {
                return collection.toSet()
            } else {
                return collection
            }
        } else {
            value.convertTo(parameter.type, parameter, false).firstOrNull()
        }
    }

    private fun applyGenerator(parameter: KParameter): Any? {
        val ann = parameter.findAnnotation<DefaultProvider<*>>()
        return if (ann != null) {
            val generator = ann.provider
            val instance = generators.getOrPut(generator) {
                generator.createInstance()
            }
            instance.get()
        } else {
            null
        }
    }

    private fun convertComposite(parameter: KParameter, form: Map<String, List<String>>): Any? {
        val type = parameter.type.classifier as KClass<*>
        val collection = (type == List::class || type == Set::class)
        val typeToUnpack = if (collection) {
            parameter.findAnnotation<FormList>()!!.type.createType().classifier as KClass<*>
        } else {
            type
        }

        val constructor = typeToUnpack.primaryConstructor
            ?: throw CannotConvertException("${parameter.name} doesn't have a primary constructor and thus can't be " +
                    "handled as a composite type")

        val missing = mutableListOf<String>()
        val subParams = constructor.parameters.associateWith { subParam ->
            if (subParam.name !in form.keys) {
                missing.add(subParam.name!!)
                null
            } else {
                form[subParam.name]!!.convertTo(subParam.type, subParam, collection)
            }
        }

        // if params are completely missing, then we don't have the object(s)
        if (missing.size == constructor.parameters.size) {
            return if (collection) {
                if (type == Set::class) {
                    emptySet()
                } else {
                    emptyList<Any?>()
                }
            } else {
                null
            }
        } else if (missing.size > 0) {
            throw CannotConvertException("Could not find properties ${missing.joinToString(",")} in form")
        }

        return if (collection) {
            var size: Int? = null
            val collected: Map<KParameter, List<Any?>> = subParams.keys.associateWith {param ->
                val results = subParams[param]
                if (size == null) {
                    size = results!!.size
                } else {
                    if (size != results!!.size) {
                        val subParamNames = subParams.keys.joinToString(",") { it.name!! }
                        throw CannotConvertException("Subtype collections have different lengths: $subParamNames")
                    }
                }
                results
            }
            val listOfResults = (0 until size!!).map { idx ->
                val elementArgs = subParams.keys.associateWith { collected[it]!![idx] }
                constructor.callBy(elementArgs)
            }
            if (type == Set::class) {
                listOfResults.toSet()
            } else {
                listOfResults
            }
        } else {
            val flat = subParams.keys.associateWith {
                (subParams[it] as List<Any?>)[0]
            }
            if (subParams.isNotEmpty()) {
                constructor.callBy(flat)
            } else {
                null
            }
        }
    }

    fun List<String>.convertTo(type: KType, parameter: KParameter, expectedCollection: Boolean): List<Any?> {
        if (size > 1 && !expectedCollection) {
            throw CannotConvertException("Multiple values specified for singular param ${parameter.name!!} of " +
                    "type $type")
        } else {
            return this.map {
                it.convertTo(type, parameter)
            }
        }
    }

    fun String?.convertTo(type: KType, parameter: KParameter): Any? {
        return when {
            type.classifier == String::class -> this.let {
                // force empty string to null; forms deliver empty string in place of null
                if (it!!.isEmpty()) {
                    null
                } else {
                    it
                }
            }
            else -> convertNonEmptyType(type, parameter)
        }
    }

    /**
     * Handle types that can't be empty strings
     */
    fun String?.convertNonEmptyType(type: KType, parameter: KParameter): Any? {
        val toConvert = this
        val clzz = type.classifier as KClass<*>
        return when {
            clzz == Int::class -> toConvert?.toIntOrNull()
            clzz == Double::class -> toConvert?.toDoubleOrNull()
            clzz == Long::class -> toConvert?.toLongOrNull()
            clzz == Boolean::class -> toConvert?.toBooleanStrictOrNull() ?:
                // handle html checkboxes :grimace:
                when (toConvert) {
                    "on" -> true
                    "" -> true
                    null -> false
                    else -> null
                }
            // TODO handle offsetdatetime (need param)
            clzz == BigDecimal::class -> toConvert?.toBigDecimalOrNull()
            clzz == UUID::class -> toConvert?.toUUID()
            clzz.isSubclassOf(Enum::class) -> {
                // Handling enum type
                @Suppress("UNCHECKED_CAST")
                val enumClass = type.classifier as KClass<Enum<*>>
                enumClass.java.enumConstants?.find {
                    it.name.equals(toConvert, ignoreCase = true)
                }
            }
            // TODO test
            else -> throw CannotConvertException("Unsupported type: $clzz for ${parameter.name}")
        }
    }

    class CannotConvertException(s: String): Exception(s)

    companion object {

        val generators = mutableMapOf<KClass<out NewValueProvider<*>>, NewValueProvider<*>>()
    }
}

interface NewValueProvider<T> {
    fun get(): T
}

class NewUUIDProvider: NewValueProvider<UUID> {
    override fun get(): UUID = UUID.randomUUID()
}

class FalseProvider: NewValueProvider<Boolean> {
    override fun get() = false
}

class TrueProvider: NewValueProvider<Boolean> {
    override fun get() = true
}

class ZeroProvider: NewValueProvider<Int> {
    override fun get() = 0
}

class OneProvider: NewValueProvider<Int> {
    override fun get() = 1
}