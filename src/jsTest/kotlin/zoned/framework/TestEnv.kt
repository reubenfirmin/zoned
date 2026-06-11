package zoned.framework

/**
 * jsTest sources run on BOTH the browser (Karma) and node targets. DOM-backed tests must guard
 * with `if (!hasDom) return` so the node run skips them instead of crashing on a missing document.
 */
internal val hasDom: Boolean
    get() = js("typeof document !== 'undefined'").unsafeCast<Boolean>()
