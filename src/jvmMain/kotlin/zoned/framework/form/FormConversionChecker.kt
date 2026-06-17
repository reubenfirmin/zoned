package zoned.framework.form

import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

class FormConversionChecker {

    val simpleTypes = setOf(String::class, Int::class, Double::class,
        Boolean::class, BigDecimal::class, UUID::class)

    data class ConversionReport(val canConvert: Boolean, val reasons: List<String>, val paramNames: List<String>) {
        fun compose(other: ConversionReport) =
            ConversionReport(canConvert || other.canConvert, reasons + other.reasons, paramNames + other.paramNames)
                .let {
                    if (it.canConvert) {
                        // clear reasons if we managed to convert
                        it.copy(reasons = listOf())
                    } else {
                        it
                    }
                }

        fun reduce(other: ConversionReport) =
            ConversionReport(canConvert && other.canConvert, reasons + other.reasons, paramNames + other.paramNames)

        fun with(other: () -> ConversionReport) =
            if (canConvert) {
                this
            } else {
                this.compose(other())
            }
    }

    fun canConvert(type: KClass<*>): ConversionReport {
        val hasOneConstructor = type.constructors.size == 1
        return if (hasOneConstructor) {
            val report = type.constructors.first().parameters.map {
                canConvert(it.type.classifier as KClass<*>, it)
            }.reduce { s, t -> s.reduce(t) }

            if (report.paramNames.toSet().size != report.paramNames.size) {
                ConversionReport(false, report.reasons
                        + ("Duplicate param names found in " + report.paramNames.joinToString(",")), report.paramNames)
            } else {
                report
            }
        } else {
            // TODO test
            ConversionReport(false, listOf("${type.qualifiedName} must have a single constructor"), listOf())
        }
    }

    fun canConvert(type: KClass<*>, parameter: KParameter): ConversionReport {

        return simpleConversion(type, parameter)
            .with { enumConversion(type, parameter)
                .with { canConvertSub(type, parameter)
                    .with { supportedCollectionConversion(type, parameter) }
                }
            }
    }

    fun simpleConversion(type: KClass<*>, parameter: KParameter): ConversionReport {
        val simpleType = type in simpleTypes
        return ConversionReport(simpleType, if (simpleType) {
            listOf()
        } else {
            listOf("${type.simpleName} is not a simple type")
        }, if (simpleType) {
            listOf(parameter.name!!)
        } else {
            listOf()
        })
    }

    fun enumConversion(type: KClass<*>, parameter: KParameter): ConversionReport {
        val enumType = type.isSubclassOf(Enum::class)
        return ConversionReport(enumType, if (enumType) {
            listOf()
        } else {
            listOf("${type.simpleName} is not an enum")
        }, if (enumType) {
            listOf(parameter.name!!)
        } else {
            listOf()
        })
    }

    fun supportedCollectionConversion(type: KClass<*>, parameter: KParameter): ConversionReport {
        val reasons = mutableListOf<String>()
        val paramNames = mutableListOf<String>()
        val supportedCollection = type in setOf(List::class, Set::class)
        if (!supportedCollection) {
            // TODO test
            reasons.add("${type.simpleName} is not a supported collection type")
        }
        val requiredAnnotation = parameter.findAnnotation<FormList>()
        val hasAnnotation = requiredAnnotation != null

        val typesCanBeHandled = if (supportedCollection && hasAnnotation) {
            val subConversion = canConvertSub(requiredAnnotation.type, parameter)
            if (subConversion.canConvert) {
                paramNames.addAll(subConversion.paramNames)
                true
            } else {
                reasons.addAll(subConversion.reasons)
                false
            }
        } else if (supportedCollection) {
            reasons.add("${type.simpleName} must be annotated with FormList with parameter use site")
            false
        } else {
            // TODO test
            false
        }

        val canConvert = supportedCollection && typesCanBeHandled
        return ConversionReport(canConvert, reasons.toList(), if (canConvert) { paramNames } else { listOf() })
    }

    fun canConvertSub(type: KClass<*>, parameter: KParameter): ConversionReport {
        // if we got here via a collection and it's a simple type
        val typeCanBeConverted = simpleConversion(type, parameter).with { enumConversion(type, parameter) }
        if (typeCanBeConverted.canConvert) {
            return typeCanBeConverted
        }

        val hasOneConstructor = type.constructors.size == 1
        if (!hasOneConstructor) {
            return ConversionReport(false,
                listOf("${type.simpleName} must have exactly one constructor in order to be convertible"), listOf()
            )
        }
        val typesCanBeHandled = type.constructors.first().parameters.map {
            val subType = it.type.classifier as KClass<*>

            simpleConversion(subType, it).with { enumConversion(subType, it) }
        }

        return if (typesCanBeHandled.all { it.canConvert }) {
            ConversionReport(true, listOf(), typesCanBeHandled.map { it.paramNames }.flatten() + parameter.name!!)
        } else {
            val report = typesCanBeHandled.reduce { s, t -> s.reduce(t)}
            report.copy(reasons = report.reasons + "Note that nested classes do not permit collections (currently)")
        }
    }

}