package zoned.gradle

import org.jooq.meta.TableDefinition
import org.jooq.meta.sqlite.SQLiteDatabase
import java.lang.reflect.Proxy

class DefaultSuppressingSqlliteConfig: SQLiteDatabase() {

    override fun getTables0() = super.getTables0()
        .map(::patch)
        .toMutableList()

    private fun patch(table: TableDefinition) = Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(TableDefinition::class.java), PatchedTableDefinition(table)
    ) as TableDefinition
}

