package zoned.gradle.generation

import org.jooq.codegen.GeneratorStrategy.Mode
import org.jooq.codegen.JavaWriter
import org.jooq.codegen.KotlinGenerator
import org.jooq.meta.DataTypeDefinition
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition
import zoned.gradle.generation.EntityGenerationStrategy
import java.io.File

class EntityKotlinGenerator : KotlinGenerator() {

    /**
     * Resolves the Kotlin type for a column, identical to the type used by the natively-generated
     * Record classes (enum classes, BigDecimal, OffsetDateTime, UUID, ...). We delegate the hard
     * part — column-type-to-class resolution — to jOOQ's own [getJavaType] (protected in
     * [org.jooq.codegen.JavaGenerator]) rather than reinventing it.
     *
     * jOOQ returns a fully-qualified *Java* type. The custom POJO writer emits a hand-built header
     * and does not participate in jOOQ's import assembly, so we keep names fully qualified (no
     * imports required) and only translate the `java.lang.*` boxed primitives to their `kotlin.*`
     * builtins — the exact correspondence the Kotlin compiler applies.
     */
    fun resolveKotlinType(type: DataTypeDefinition, out: JavaWriter): String =
        kotlinBuiltinType(getJavaType(type, out))

    private lateinit var entityStrategy: EntityGenerationStrategy

    override fun setStrategy(strategy: org.jooq.codegen.GeneratorStrategy) {
        super.setStrategy(strategy)
        if (strategy is EntityGenerationStrategy) {
            entityStrategy = strategy
        } else {
            println("Warning: Strategy is not an EntityGenerationStrategy, falling back to standard generation!")
        }
    }

    // This overrides the protected method in KotlinGenerator
    override fun generatePojo(table: TableDefinition) {
        val file = getFile(table, Mode.POJO)
        val out = newJavaWriter(file)
        println("Generating custom POJO: " + out.file().name)

        try {
            // Use our custom strategy if available
            if (::entityStrategy.isInitialized) {
                val content = entityStrategy.generateEntitySealedClass(this, table, out)
                out.print(content)
            } else {
                // Fall back to default implementation
                generatePojo(table, out)
            }
        } catch (e: Exception) {
            println("Error generating custom entity for table ${table.name}: ${e.message}")
            e.printStackTrace()

            // Try to continue with default generation
            generatePojo(table, out)
        } finally {
            closeJavaWriter(out)
        }
    }

    // These methods override the protected methods in KotlinGenerator
    override fun generatePojoEqualsAndHashCode(tableOrUDT: Definition, out: JavaWriter) {
        // Do nothing - data classes handle this
    }

    override fun generatePojoToString(tableOrUDT: Definition, out: JavaWriter) {
        // Do nothing - data classes handle this
    }
}

/**
 * Translates a fully-qualified `java.lang.*` boxed primitive (as returned by jOOQ's `getJavaType`)
 * into its `kotlin.*` builtin equivalent. Any other type (project enums, `java.util.UUID`,
 * `java.time.OffsetDateTime`, `java.math.BigDecimal`, ...) is already a valid fully-qualified Kotlin
 * type and is returned unchanged. This is the exact correspondence the Kotlin compiler itself
 * applies to Java types, so the generated POJOs avoid the "use kotlin.String instead" warnings that
 * would otherwise fail compilation under `allWarningsAsErrors`.
 */
internal fun kotlinBuiltinType(javaType: String): String = when (javaType) {
    "java.lang.String" -> "kotlin.String"
    "java.lang.Integer" -> "kotlin.Int"
    "java.lang.Long" -> "kotlin.Long"
    "java.lang.Short" -> "kotlin.Short"
    "java.lang.Byte" -> "kotlin.Byte"
    "java.lang.Double" -> "kotlin.Double"
    "java.lang.Float" -> "kotlin.Float"
    "java.lang.Boolean" -> "kotlin.Boolean"
    "java.lang.Character" -> "kotlin.Char"
    "java.lang.Object" -> "kotlin.Any"
    "byte[]" -> "kotlin.ByteArray"
    else -> javaType
}