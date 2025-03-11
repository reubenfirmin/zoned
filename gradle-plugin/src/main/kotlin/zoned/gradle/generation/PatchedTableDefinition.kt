package zoned.gradle.generation

import org.jooq.meta.DataTypeDefinition
import org.jooq.meta.DefaultColumnDefinition
import org.jooq.meta.TableDefinition
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

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
        DefaultColumnDefinition(
            tableDef, columnDef.name, columnDef.position, Proxy.newProxyInstance(
                javaClass.classLoader,
                arrayOf(DataTypeDefinition::class.java),
                PatchedDataType(tableDef, columnDef, columnDef.type)
            ) as DataTypeDefinition, columnDef.isIdentity, columnDef.isReadonly, columnDef.comment
        )
}