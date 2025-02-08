package zoned.gradle

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