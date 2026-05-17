package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.Script

interface MonitoringRepository {
    fun startMonitoring(script: Script): Int?

    fun stopMonitoring(scriptId: Int)

    fun stopAllMonitoring()

    fun hasNotificationPermission(): Boolean
}
