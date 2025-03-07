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
        val methodName = method.name
        val type = original.type
        val isDecimal = type.equals("decimal", ignoreCase = true) ||
                type.equals("numeric", ignoreCase = true) ||
                type.equals("number", ignoreCase = true)

        // Handle decimal type conversion
        if (isDecimal) {
            when (methodName) {
                "getSQLDataType" -> {
                    // Let the original method run, we'll just log it to debug
                    val result = method.invoke(original, *(args ?: emptyArray()))
                    println("SQLDataType for ${tableDef.name}.${columnDef.name}: $result")
                    return result
                }
                "getType" -> return "double"
                "getUserType" -> return "kotlin.Double"
                "getConverter" -> return null
                "getBinding" -> return null
                "isGenericNumberType" -> return false
                "isNumeric" -> return true
                "getLength" -> return 0
                "getPrecision" -> return 0
                "getScale" -> return 0
            }
        }

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