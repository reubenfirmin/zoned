package zoned.framework

import kotlinx.css.Color
import web.timers.setTimeout

object Debugger {

    var debug = false
    var profile = false

    fun debugOn() {
        debug = true
    }

    fun profileOn() {
        profile = true
    }

    fun debug(s: String, logType: LogType? = null) {
        if (debug) {
            profileMarkerStart(s)
            console.log("%c $s", when (logType) {
                null -> ""
                else -> "background: #${logType.backgroundColor}; #{${logType.textColor} [${logType.name}] "
            })
            setTimeout({ profileMarkerEnd(s) }, 100)
        }
    }

    fun profileMarkerStart(s: String) {
        if (profile) {
            console.asDynamic().time(s)
        }
    }

    fun profileMarkerEnd(s: String) {
        if (profile) {
            console.asDynamic().timeEnd(s)
        }
    }
}

data class LogType(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color
)