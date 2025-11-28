package zoned.framework.interop

/**
 * URL encoding/decoding utilities.
 * These are browser globals, typed here for Kotlin/JS.
 */

external fun encodeURIComponent(str: String): String
external fun decodeURIComponent(str: String): String
