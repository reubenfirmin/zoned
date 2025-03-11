package zoned.gradle.generation

import org.jooq.meta.TableDefinition
import org.jooq.meta.postgres.PostgresDatabase
import java.lang.reflect.Proxy

class DefaultSuppressingPostgresConfig: PostgresDatabase() {

    override fun getTables0() = super.getTables0()
        .map(::patch)
        .toMutableList()

    private fun patch(table: TableDefinition) = Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(TableDefinition::class.java), PatchedTableDefinition(table)
    ) as TableDefinition
}

