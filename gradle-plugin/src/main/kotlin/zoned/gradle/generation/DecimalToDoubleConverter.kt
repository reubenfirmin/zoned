package zoned.gradle.generation

import org.jooq.Converter
import java.math.BigDecimal

class DecimalToDoubleConverter : Converter<BigDecimal, Double> {
    override fun from(databaseObject: BigDecimal?): Double? {
        return databaseObject?.toDouble()
    }

    override fun to(userObject: Double?): BigDecimal? {
        return userObject?.let { BigDecimal.valueOf(it) }
    }

    override fun fromType(): Class<BigDecimal> = BigDecimal::class.java
    override fun toType(): Class<Double> = Double::class.java
}