package zoned.gradle.generation

import org.jooq.meta.ColumnDefinition
import org.jooq.meta.DataTypeDefinition
import org.jooq.meta.TableDefinition
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class PatchedDataType(
    private val tableDef: TableDefinition,
    private val columnDef: ColumnDefinition,
    private val original: DataTypeDefinition
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
        val methodName = method.name

        // Special case for nullability
        if (methodName == "isDefaulted") {
            val result = method.invoke(original, *(args ?: emptyArray())) as? Boolean
            if (result == true) {
                println("Turning off nullability for ${tableDef.name}.${columnDef.name}")
            }
            return false
        }

        // For all other methods, delegate to the original
        return method.invoke(original, *(args ?: emptyArray()))
    }
}