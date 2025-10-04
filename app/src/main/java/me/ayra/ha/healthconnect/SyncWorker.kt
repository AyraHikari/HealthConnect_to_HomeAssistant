package me.ayra.ha.healthconnect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import me.ayra.ha.healthconnect.data.Settings.getAutoSync
import me.ayra.ha.healthconnect.data.Settings.getForegroundServiceEnabled
import me.ayra.ha.healthconnect.data.Settings.getSettings
import me.ayra.ha.healthconnect.data.Settings.removeLastError
import me.ayra.ha.healthconnect.data.Settings.setLastError
import me.ayra.ha.healthconnect.data.Settings.setLastSync
import me.ayra.ha.healthconnect.data.Settings.setSettings
import me.ayra.ha.healthconnect.network.HomeAssistant.sendToHomeAssistant
import me.ayra.ha.healthconnect.utils.DataStore.toJson
import me.ayra.ha.healthconnect.utils.HealthConnectManager
import me.ayra.ha.healthconnect.utils.HealthData
import me.ayra.ha.healthconnect.utils.TimeUtils.unixTimeMs
import java.util.concurrent.TimeUnit
import me.ayra.ha.healthconnect.data.Settings as DataSettings

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "SyncWorker"
        const val DEFAULT_INTERVAL = 3600L
        const val MINIMUM_INTERVAL = 15 * 60L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val FOREGROUND_CHANNEL_ID = "health_data_sync"
        private const val FOREGROUND_NOTIFICATION_ID = 1001

        fun startNow(context: Context?) {
            if (context == null) return

            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val oneTimeSyncDataWork =
                OneTimeWorkRequest
                    .Builder(SyncWorker::class.java)
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueue(oneTimeSyncDataWork)
        }

        fun schedule(context: Context) {
            val interval =
                context.getSettings("updateInterval")?.toString()?.toLongOrNull()
                    ?: DEFAULT_INTERVAL
            val constrainedInterval = interval.coerceAtLeast(MINIMUM_INTERVAL)

            val workManager = WorkManager.getInstance(context)
            if (getCurrentInterval(workManager) == interval) return

            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true) // Add battery constraint
                    .build()

            val syncRequest =
                PeriodicWorkRequestBuilder<SyncWorker>(
                    constrainedInterval,
                    TimeUnit.SECONDS,
                ).setConstraints(constraints)
                    .addTag(TAG)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                "HealthConnectSync",
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest,
            )
        }

        private fun getCurrentInterval(workManager: WorkManager): Long? =
            try {
                workManager
                    .getWorkInfosByTag(TAG)
                    .get()
                    .firstOrNull { it.state == WorkInfo.State.ENQUEUED }
                    ?.progress
                    ?.getLong("INTERVAL", -1)
                    ?.takeIf { it != -1L }
            } catch (e: Exception) {
                null
            }
    }

    override suspend fun doWork(): Result {
        if (applicationContext.getAutoSync() == false) return Result.success()

        val context = applicationContext
        val useForegroundNotification = context.getForegroundServiceEnabled()
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        if (useForegroundNotification) {
            setForeground(createForegroundInfo(notificationManager))
        } else {
            showSyncNotification(notificationManager)
        }

        var attempt = 0
        var lastError: Exception? = null

        try {
            while (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    return trySync()
                } catch (e: RemoteException) {
                    lastError = e
                    Log.w(
                        TAG,
                        "Health Connect binder error (attempt ${attempt + 1}/$MAX_RETRY_ATTEMPTS)",
                        e,
                    )
                    attempt++
                    delay(1000L * (attempt + 1)) // Exponential backoff
                } catch (e: Exception) {
                    lastError = e
                    break
                }
            }
            context.setLastError(
                DataSettings.SyncError(
                    unixTimeMs,
                    lastError?.message ?: "Unknown error",
                ),
            )
            return Result.failure()
        } finally {
            if (!useForegroundNotification) {
                cancelSyncNotification(notificationManager)
            }
        }
    }

    private suspend fun trySync(): Result {
        val context = applicationContext

        // 1. Check permissions
        val hc =
            HealthConnectManager(context).apply {
                if (!hasAllPermissions()) {
                    Log.w(TAG, "Missing Health Connect permissions")
                    return Result.failure()
                }
            }

        // 2. Get health data with retry logic
        val healthData =
            try {
                HealthData(context).getHealthData(hc).also {
                    context.setSettings("health_data", it.toJson())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update health data", e)
                throw e
            }

        // 3. Send to Home Assistant
        val (isSuccess, message) =
            try {
                sendToHomeAssistant(
                    healthData,
                    entityId =
                        context.getSettings("sensor") ?: run {
                            Log.w(TAG, "No sensor entity ID configured")
                            return Result.failure()
                        },
                    apiUrl =
                        context.getSettings("url") ?: run {
                            Log.w(TAG, "No Home Assistant URL configured")
                            return Result.failure()
                        },
                    apiToken =
                        context.getSettings("token") ?: run {
                            Log.w(TAG, "No Home Assistant token configured")
                            return Result.failure()
                        },
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send data to Home Assistant", e)
                throw e
            }

        if (isSuccess) {
            context.setLastSync(unixTimeMs)
            context.removeLastError()
            return Result.success()
        } else {
            throw RuntimeException("Home Assistant error: $message")
        }
    }

    private fun createForegroundInfo(notificationManager: NotificationManager?): ForegroundInfo =
        ForegroundInfo(FOREGROUND_NOTIFICATION_ID, buildNotification(notificationManager))

    private fun showSyncNotification(notificationManager: NotificationManager?) {
        if (notificationManager == null) {
            Log.w(TAG, "NotificationManager not available; unable to display sync notification")
            return
        }
        notificationManager.notify(
            FOREGROUND_NOTIFICATION_ID,
            buildNotification(notificationManager),
        )
    }

    private fun cancelSyncNotification(notificationManager: NotificationManager?) {
        notificationManager?.cancel(FOREGROUND_NOTIFICATION_ID)
    }

    private fun buildNotification(notificationManager: NotificationManager?): Notification {
        val context = applicationContext
        val channelId = FOREGROUND_CHANNEL_ID

        val channel =
            NotificationChannel(
                channelId,
                context.getString(R.string.foreground_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.foreground_notification_channel_description)
                setShowBadge(false)
            }
        notificationManager?.createNotificationChannel(channel)

        val settingsIntent =
            Intent().apply {
                action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                settingsIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notificationText = context.getString(R.string.foreground_notification_text)

        return NotificationCompat
            .Builder(context, channelId)
            .setContentTitle(context.getString(R.string.foreground_notification_title))
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setSmallIcon(R.drawable.ic_sync_24px)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
