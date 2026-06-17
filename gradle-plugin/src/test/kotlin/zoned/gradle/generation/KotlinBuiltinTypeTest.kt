package zoned.gradle.generation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the [kotlinBuiltinType] mapping that translates the fully-qualified `java.lang.*`
 * boxed primitives jOOQ returns from `getJavaType` into their `kotlin.*` builtins. Getting this
 * wrong reintroduces the "use kotlin.String instead" warnings that fail compilation under
 * `allWarningsAsErrors` in consuming projects.
 */
class KotlinBuiltinTypeTest {

    @Test
    fun `boxed java primitives map to kotlin builtins`() {
        assertEquals("kotlin.String", kotlinBuiltinType("java.lang.String"))
        assertEquals("kotlin.Int", kotlinBuiltinType("java.lang.Integer"))
        assertEquals("kotlin.Long", kotlinBuiltinType("java.lang.Long"))
        assertEquals("kotlin.Short", kotlinBuiltinType("java.lang.Short"))
        assertEquals("kotlin.Byte", kotlinBuiltinType("java.lang.Byte"))
        assertEquals("kotlin.Double", kotlinBuiltinType("java.lang.Double"))
        assertEquals("kotlin.Float", kotlinBuiltinType("java.lang.Float"))
        assertEquals("kotlin.Boolean", kotlinBuiltinType("java.lang.Boolean"))
        assertEquals("kotlin.Char", kotlinBuiltinType("java.lang.Character"))
        assertEquals("kotlin.Any", kotlinBuiltinType("java.lang.Object"))
        assertEquals("kotlin.ByteArray", kotlinBuiltinType("byte[]"))
    }

    @Test
    fun `non-builtin fully qualified types are returned unchanged`() {
        // Project enums, JDK value types and BigDecimal are already valid Kotlin types.
        assertEquals("java.util.UUID", kotlinBuiltinType("java.util.UUID"))
        assertEquals("java.time.OffsetDateTime", kotlinBuiltinType("java.time.OffsetDateTime"))
        assertEquals("java.time.LocalDate", kotlinBuiltinType("java.time.LocalDate"))
        assertEquals("java.math.BigDecimal", kotlinBuiltinType("java.math.BigDecimal"))
        assertEquals("rcp.model.jooq.enums.Currency", kotlinBuiltinType("rcp.model.jooq.enums.Currency"))
    }

    @Test
    fun `already-kotlin types are left alone`() {
        assertEquals("kotlin.String", kotlinBuiltinType("kotlin.String"))
        assertEquals("kotlin.Double", kotlinBuiltinType("kotlin.Double"))
    }
}
