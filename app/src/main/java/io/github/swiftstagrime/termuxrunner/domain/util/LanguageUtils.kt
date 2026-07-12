package io.github.swiftstagrime.termuxrunner.domain.util
import androidx.hilt.navigation.compose.hiltViewModel

object LanguageUtils {
    fun getCommentSymbol(interpreter: String): String =
        when (interpreter.trim().lowercase()) {
            "python", "python3", "ruby", "perl" -> "#"
            "node", "nodejs", "js", "javascript" -> "//"
            "lua" -> "--"
            else -> "#"
        }
}
