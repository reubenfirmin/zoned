package zoned.framework.util

import java.util.*

fun String?.toUUID() =
    if (this == null) { null } else { UUID.fromString(this) }

