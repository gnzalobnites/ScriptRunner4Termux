package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.service.HeartbeatService
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.MonitoringRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing monitoring-related operations.
 * Binds to [HeartbeatService] to retrieve TCP ports for script monitoring.
 */
@Singleton
class MonitoringRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MonitoringRepository {
        override fun hasNotificationPermission(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        override fun startMonitoring(script: Script): Int? {
            if (!hasNotificationPermission()) return null

            val startIntent =
                Intent(context, HeartbeatService::class.java).apply {
                    action = HeartbeatService.ACTION_START
                    putExtra(HeartbeatService.EXTRA_SCRIPT_ID, script.id)
                    putExtra(HeartbeatService.EXTRA_SCRIPT_NAME, script.name)
                    putExtra(HeartbeatService.EXTRA_TIMEOUT_MS, script.heartbeatTimeout)
                }

            ContextCompat.startForegroundService(context, startIntent)

            return bindAndGetPort(script.id)
        }

        private fun bindAndGetPort(scriptId: Int): Int? {
            val latch = CountDownLatch(1)
            var port: Int? = null

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    try {
                        val binder = service as HeartbeatService.ScriptPortBinder
                        port = binder.getPort(scriptId)
                    } catch (e: Exception) {
                    } finally {
                        latch.countDown()
                        try {
                            context.unbindService(this)
                        } catch (_: Exception) {
                        }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    latch.countDown()
                }
            }

            val bindIntent = Intent(context, HeartbeatService::class.java)
            val bound = context.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)

            if (bound) {
                latch.await(5, TimeUnit.SECONDS)
            }

            return port
        }

        override fun stopMonitoring(scriptId: Int) {
            val intent =
                Intent(context, HeartbeatService::class.java).apply {
                    action = HeartbeatService.ACTION_STOP
                    putExtra(HeartbeatService.EXTRA_SCRIPT_ID, scriptId)
                }
            context.stopService(intent)
        }

        override fun stopAllMonitoring() {
            val intent =
                Intent(context, HeartbeatService::class.java).apply {
                    action = HeartbeatService.ACTION_STOP_ALL
                }
            context.stopService(intent)
        }
    }
