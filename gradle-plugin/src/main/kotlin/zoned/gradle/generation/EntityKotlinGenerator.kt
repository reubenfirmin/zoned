package zoned.gradle.generation

import org.jooq.codegen.GeneratorStrategy.Mode
import org.jooq.codegen.JavaWriter
import org.jooq.codegen.KotlinGenerator
import org.jooq.meta.Definition
import org.jooq.meta.TableDefinition
import zoned.gradle.generation.EntityGenerationStrategy
import java.io.File

class EntityKotlinGenerator : KotlinGenerator() {

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