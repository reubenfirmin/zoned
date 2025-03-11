package zoned.framework.i18n

import io.javalin.http.Context
import zoned.framework.i18n.LanguageHandler.lang
import zoned.framework.i18n.LanguageHandler.withLanguage

object LanguageHandler {

    fun withLanguage(ctx: Context): Language {
        val langChoice = ctx.header("Accept-Language")
        return if (langChoice.isNullOrBlank() || langChoice.lowercase().startsWith("en")) {
            Language.ENGLISH
        } else {
            Language.ESPANOL
        }
    }

    // shim for this two language project
    fun lang(language: Language, english: String, espanol: String) =
        if (language == Language.ENGLISH) {
            english
        } else {
            espanol
        }
}

data class Translated(val english: String, val espanol: String)

fun Context.language() = withLanguage(this)

fun Context.lang(english: String, espanol: String) = lang(this.language(), english, espanol)

fun Context.lang(translated: Translated) = lang(this.language(), translated.english, translated.espanol)

fun lang(language: Language, english: String, espanol: String) = lang(language, english, espanol)

fun lang(language: Language, translated: Translated) = lang(language, translated.english, translated.espanol)

enum class Language {
    ENGLISH,
    ESPANOL
}