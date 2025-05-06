package me.ayra.ha.healthconnect

import android.content.Context
import android.os.RemoteException
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import me.ayra.ha.healthconnect.data.Settings
import me.ayra.ha.healthconnect.data.Settings.getAutoSync
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

class SyncWorker(context: Context, workerParams: WorkerParameters)
    : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        const val DEFAULT_INTERVAL = 3600L
        const val MINIMUM_INTERVAL = 15 * 60L
        private const val MAX_RETRY_ATTEMPTS = 3

        fun startNow(context: Context?) {
            if (context == null) return

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeSyncDataWork =
                OneTimeWorkRequest.Builder(SyncWorker::class.java)
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueue(oneTimeSyncDataWork)
        }

        fun schedule(context: Context) {
            val interval = context.getSettings("updateInterval")?.toString()?.toLongOrNull()
                ?: DEFAULT_INTERVAL
            val constrainedInterval = interval.coerceAtLeast(MINIMUM_INTERVAL)

            val workManager = WorkManager.getInstance(context)
            if (getCurrentInterval(workManager) == interval) return

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true) // Add battery constraint
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                constrainedInterval,
                TimeUnit.SECONDS
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "HealthConnectSync",
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )
        }

        private fun getCurrentInterval(workManager: WorkManager): Long? {
            return try {
                workManager.getWorkInfosByTag(TAG).get()
                    .firstOrNull { it.state == WorkInfo.State.ENQUEUED }
                    ?.progress?.getLong("INTERVAL", -1)
                    ?.takeIf { it != -1L }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun doWork(): Result {
        if (applicationContext.getAutoSync() == false) return Result.success()

        var attempt = 0
        var lastError: Exception? = null

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                return trySync()
            } catch (e: RemoteException) {
                lastError = e
                Log.w(TAG, "Health Connect binder error (attempt ${attempt + 1}/$MAX_RETRY_ATTEMPTS)", e)
                attempt++
                delay(1000L * (attempt + 1)) // Exponential backoff
            } catch (e: Exception) {
                lastError = e
                break
            }
        }

        applicationContext.setLastError(Settings.SyncError(
            unixTimeMs,
            lastError?.message ?: "Unknown error"
        ))
        return Result.failure()
    }

    private suspend fun trySync(): Result {
        val context = applicationContext

        // 1. Check permissions
        val hc = HealthConnectManager(context).apply {
            if (!hasAllPermissions()) {
                Log.w(TAG, "Missing Health Connect permissions")
                return Result.failure()
            }
        }

        // 2. Get health data with retry logic
        val healthData = try {
            HealthData(context).getHealthData(hc).also {
                context.setSettings("health_data", it.toJson())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update health data", e)
            throw e
        }

        // 3. Send to Home Assistant
        val (isSuccess, message) = try {
            sendToHomeAssistant(
                healthData,
                entityId = context.getSettings("sensor") ?: run {
                    Log.w(TAG, "No sensor entity ID configured")
                    return Result.failure()
                },
                apiUrl = context.getSettings("url") ?: run {
                    Log.w(TAG, "No Home Assistant URL configured")
                    return Result.failure()
                },
                apiToken = context.getSettings("token") ?: run {
                    Log.w(TAG, "No Home Assistant token configured")
                    return Result.failure()
                }
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
}