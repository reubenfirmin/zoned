package zoned.gradle

import org.gradle.configurationcache.extensions.capitalized
import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.codegen.GeneratorStrategy
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition

class RenamingStrategy() : DefaultGeneratorStrategy() {

    override fun getJavaClassImplements(definition: Definition?, mode: GeneratorStrategy.Mode?): MutableList<String> {
        val defs = super.getJavaClassImplements(definition, mode)
        val addl = when (mode) {
            GeneratorStrategy.Mode.RECORD ->  "zoned.framework.db.Record"
            GeneratorStrategy.Mode.POJO -> "zoned.framework.db.Entity"
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

    override fun getJavaClassName(definition: Definition?, mode: GeneratorStrategy.Mode?): String {
        val defaultName = super.getJavaClassName(definition, mode)
        return defaultName.singularize()
    }

    override fun getJavaIdentifier(definition: Definition): String {
        val identifier = super.getJavaIdentifier(definition)
        return identifier.split('_')
            .joinToString("") { it.lowercase().capitalized() }
    }

    fun String.singularize() = if (exceptions.contains(this)) { this } else { this.removeSuffix("s") }

    val exceptions = setOf<String>()
}