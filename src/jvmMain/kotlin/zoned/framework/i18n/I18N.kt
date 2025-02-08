package zoned.framework.i18n

import com.google.common.collect.HashBasedTable
import org.slf4j.LoggerFactory
import java.util.*

class I18N {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val bundles = HashBasedTable.create<Locale, String, ResourceBundle>()
    }

    private fun loadBundle(name: String, locale: Locale): ResourceBundle? =
        ResourceBundle.getBundle("i18n/$name", locale)!!

    private fun getBundle(name: String, locale: Locale): ResourceBundle {
        var bundle = bundles.get(name, locale)
        if (bundle != null) return bundle

        bundle = loadBundle(name, locale)
        if (bundle != null) {
            bundles.put(locale, name, bundle)
            return bundle
        }

        bundle = loadBundle(name, Locale.ENGLISH)
        if (bundle != null) {
            logger.warn("Falling back to english bundle for $name")
            bundles.put(locale, name, bundle)
            return bundle
        }

        throw Exception("Could not find bundle for $name")
    }

    fun str(locale: Locale, name: String, key: String): String {
        val bundle = getBundle(name, locale)
        val value = bundle.getString(key)
        return value
    }

    fun bundle(name: String, locale: Locale) = object: Bundle {
        override fun str(key: String) = str(locale, name, key)
    }

    @FunctionalInterface
    interface Bundle {
        fun str(key: String): String
    }
}