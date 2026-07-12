package io.github.swiftstagrime.termuxrunner.domain.repository
import androidx.hilt.navigation.compose.hiltViewModel

interface IconRepository {
    suspend fun saveIcon(uriStr: String): String?
}
