package zoned.gradle

import org.jooq.meta.ColumnDefinition
import org.jooq.meta.DataTypeDefinition
import org.jooq.meta.DefaultColumnDefinition
import org.jooq.meta.TableDefinition
import org.jooq.meta.postgres.PostgresDatabase
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class DefaultSuppressingPostgresConfig: PostgresDatabase() {

    override fun getTables0() = super.getTables0()
        .map(::patch)
        .toMutableList()

    private fun patch(table: TableDefinition) = Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(TableDefinition::class.java), PatchedTableDefinition(table)
    ) as TableDefinition
}

class PatchedTableDefinition(
    private val tableDef: TableDefinition
) : InvocationHandler {

    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any? {
        val result = method.invoke(tableDef, *(args ?: emptyArray()))

        if (method.getName().startsWith("getColumn")) {
            return when (result) {
                is List<*> -> patch(result)
                is DefaultColumnDefinition -> patch(result)
                else -> throw Exception("Unhandled result type: $result")
            }
        }

        return result
    }

    private fun patch(columnDefs: List<*>) = columnDefs.map {
        patch(it as DefaultColumnDefinition)
    }

    private fun patch(columnDef: DefaultColumnDefinition) =
        DefaultColumnDefinition(tableDef, columnDef.name, columnDef.position, Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(DataTypeDefinition::class.java),
            PatchedDataType(tableDef, columnDef, columnDef.type)
        ) as DataTypeDefinition, columnDef.isIdentity, columnDef.isReadonly, columnDef.comment)
}

class PatchedDataType(
    private val tableDef: TableDefinition,
    private val columnDef: ColumnDefinition,
    private val original: DataTypeDefinition
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
        val result = method.invoke(original, *(args ?: emptyArray()))

        if (method.name == "isDefaulted") {
            if (result is Boolean && result) {
                println("Turning off nullability for ${tableDef.name}.${columnDef.name}")
            }
            return false
        }

        return result
    }
}