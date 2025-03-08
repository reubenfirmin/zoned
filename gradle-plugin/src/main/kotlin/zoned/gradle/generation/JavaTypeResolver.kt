package zoned.gradle.generation

import org.jooq.meta.DataTypeDefinition

/**
 * Helper class to resolve Java types from DataTypeDefinitions
 * when using the JOOQ code generator.
 */
class JavaTypeResolver() {

    /**
     * Get the Java type for a DataTypeDefinition
     */
    fun getJavaType(type: DataTypeDefinition): String {
            val typeName = type.type.lowercase()

        return when {
            // Handle common SQL types
            typeName.contains("int") -> "Int"
            typeName.contains("varchar") || typeName.contains("char") || typeName.contains("text") -> "String"
            typeName.contains("decimal") || typeName.contains("numeric") -> "Double"
            typeName.contains("timestamp") -> "java.time.LocalDateTime"
            typeName.contains("datetime") -> "java.time.LocalDateTime"
            typeName.contains("date") -> "java.time.LocalDate"
            typeName.contains("time") -> "java.time.LocalTime"
            typeName.contains("boolean") || typeName.contains("bit") -> "Boolean"
            typeName.contains("real") || typeName.contains("float") -> "Float"
            typeName.contains("double") -> "Double"
            typeName.contains("long") || typeName.contains("bigint") -> "Long"
            typeName.contains("binary") || typeName.contains("blob") -> "ByteArray"
            typeName.contains("uuid") -> "java.util.UUID"

            // Use user type if available
            type.userType != null -> type.userType

            // Fallback to default
            else -> "Any"
        }
    }
}