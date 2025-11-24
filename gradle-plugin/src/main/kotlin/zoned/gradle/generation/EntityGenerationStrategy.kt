package zoned.gradle.generation

import org.gradle.configurationcache.extensions.capitalized
import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.codegen.GeneratorStrategy
import org.jooq.codegen.KotlinGenerator
import org.jooq.meta.ColumnDefinition
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition

class EntityGenerationStrategy : DefaultGeneratorStrategy() {

    // Metadata fields to exclude from entity classes
    private val metadataFields = setOf(
        "created",
        "modified",
        "deleted"
    )

    private val typeResolver = JavaTypeResolver()

    // Exceptions for singularization
    private val singularizeExceptions = setOf<String>()

    // Check if a field should be excluded (metadata field)
    private fun isMetadataField(definition: Definition): Boolean {
        if (definition !is ColumnDefinition) return false
        return metadataFields.contains(definition.name.lowercase())
    }

    // Get all non-metadata columns for a table
    private fun getNonMetadataColumns(table: TableDefinition): List<ColumnDefinition> {
        return table.columns.filter { !isMetadataField(it) }
    }

    // Get primary key column for a table
    private fun getPrimaryKeyColumn(table: TableDefinition): ColumnDefinition? {
        val primaryKey = table.primaryKey ?: return null
        // Use getKeyColumns() to get the list of columns in the primary key
        return primaryKey.getKeyColumns().firstOrNull()
    }

    // Standard Java/Kotlin identifier conversion
    // This is used for default jOOQ generated code - tables, records, etc.
    override fun getJavaIdentifier(definition: Definition): String {
        val identifier = super.getJavaIdentifier(definition)
        return identifier.split('_')
            .joinToString("") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    // For entity POJO fields, we want camelCase instead of PascalCase
    fun getJavaIdentifierForEntityField(definition: Definition): String {
        val pascalCase = getJavaIdentifier(definition)
        // Convert first letter to lowercase for entity properties
        return pascalCase.first().lowercase() + pascalCase.substring(1)
    }

    // From RenamingStrategy - singularize class names
    override fun getJavaClassName(definition: Definition?, mode: GeneratorStrategy.Mode?): String {
        val defaultName = super.getJavaClassName(definition, mode)
        return defaultName.singularize()
    }

    fun String.singularize() = if (singularizeExceptions.contains(this)) { this } else { this.removeSuffix("s") }

    private fun isNullable(column: ColumnDefinition): Boolean {
        return column.type.isNullable
    }

    fun hasPrimaryKeyId(table: TableDefinition): Boolean {
        val pkColumn = getPrimaryKeyColumn(table)
        return pkColumn != null && getJavaIdentifierForEntityField(pkColumn) == "id"
    }

    // From RenamingStrategy - add appropriate interfaces
    override fun getJavaClassImplements(definition: Definition?, mode: GeneratorStrategy.Mode?): MutableList<String> {
        val defs = super.getJavaClassImplements(definition, mode)
        val addl = when (mode) {
            GeneratorStrategy.Mode.RECORD ->  "zoned.framework.db.Record"
            GeneratorStrategy.Mode.POJO ->
//                if (definition is TableDefinition && hasPrimaryKeyId(definition)) {
//                null // Don't add Entity interface here for POJO when using sealed class
//            } else {
                "zoned.framework.db.Entity"
//            }
            GeneratorStrategy.Mode.DEFAULT -> {
                if (definition is TableDefinition) {
                    "zoned.framework.db.EntityTable"
                } else {
                    null
                }
            }
            else -> null
        }
        if (addl != null) {
            return (defs + addl).toMutableList()
        }
        return defs
    }

    // Generate custom POJO class for entities with the sealed class pattern
    fun generateEntitySealedClass(generator: KotlinGenerator, table: TableDefinition, out: org.jooq.codegen.JavaWriter): String {
        val className = getJavaClassName(table, GeneratorStrategy.Mode.POJO)
        val pkColumn = getPrimaryKeyColumn(table)
        val nonMetadataColumns = getNonMetadataColumns(table)

        val sb = StringBuilder()

        // Add header and imports - we'll handle these manually
        sb.append("package ${generator.getTargetPackage()}.tables.pojos;\n\n")
        sb.append("import java.io.Serializable;\n")
        sb.append("import zoned.framework.db.Entity;\n\n")

        // Use sealed class pattern for any table with an "id" primary key field
        val useSealedClass = pkColumn != null && getJavaIdentifierForEntityField(pkColumn) == "id"

        // Exclude primary key column from New state
        val newStateColumns = if (pkColumn != null) {
            nonMetadataColumns.filter { it != pkColumn }
        } else {
            nonMetadataColumns
        }

        if (useSealedClass) {
            // Generate sealed class for id primary key case
            sb.append("/**\n * This class is generated by jOOQ.\n */\n")
            sb.append("@Suppress(\"UNCHECKED_CAST\")\n")
            sb.append("sealed class $className : Entity, Serializable {\n")

            // New state class
            sb.append("    data class New(\n")
            newStateColumns.forEachIndexed { index, column ->
                val fieldName = getJavaIdentifierForEntityField(column)
                // Use the JavaTypeResolver to get the correct Kotlin type
                val fieldType = typeResolver.getJavaType(column.getType())
                // Add nullable marker if the column is nullable
                val fieldTypeWithNullability = if (isNullable(column)) "$fieldType?" else fieldType
                sb.append("        val $fieldName: $fieldTypeWithNullability")
                if (index < newStateColumns.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("    ) : $className()\n\n")

            // Existing state class
            sb.append("    data class Existing(\n")
            if (pkColumn != null) {
                val pkFieldName = getJavaIdentifierForEntityField(pkColumn)
                val pkFieldType = typeResolver.getJavaType(pkColumn.getType())
                sb.append("        val $pkFieldName: $pkFieldType,\n")
            }

            newStateColumns.forEachIndexed { index, column ->
                val fieldName = getJavaIdentifierForEntityField(column)
                val fieldType = typeResolver.getJavaType(column.getType())
                // Add nullable marker if the column is nullable
                val fieldTypeWithNullability = if (isNullable(column)) "$fieldType?" else fieldType
                sb.append("        val $fieldName: $fieldTypeWithNullability")
                if (index < newStateColumns.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("    ) : $className()\n")

            sb.append("}\n")
        } else {
            // Generate standard data class for tables without id primary key
            sb.append("/**\n * This class is generated by jOOQ, via Zoned Framework.\n */\n")
            sb.append("@Suppress(\"UNCHECKED_CAST\")\n")
            sb.append("data class $className(\n")

            // Include all non-metadata columns
            nonMetadataColumns.forEachIndexed { index, column ->
                val fieldName = getJavaIdentifierForEntityField(column)
                val fieldType = typeResolver.getJavaType(column.getType())
                val fieldTypeWithNullability = if (isNullable(column)) "$fieldType?" else fieldType
                sb.append("        val $fieldName: $fieldTypeWithNullability")
                if (index < nonMetadataColumns.size - 1) sb.append(",")
                sb.append("\n")
            }

            sb.append("): Entity, Serializable {\n")
            sb.append("}\n")
        }

        return sb.toString()
    }
}