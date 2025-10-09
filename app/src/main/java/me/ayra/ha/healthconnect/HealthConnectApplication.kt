package me.ayra.ha.healthconnect

import android.app.Application
import com.google.android.material.color.DynamicColors
import me.ayra.ha.healthconnect.utils.CrashLogLifecycleCallbacks
import me.ayra.ha.healthconnect.utils.CrashLogger

class HealthConnectApplication : Application() {
    private lateinit var crashLogger: CrashLogger

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        crashLogger =
            CrashLogger(this).also { logger ->
                logger.install()
                if (logger.hasPendingCrashLog()) {
                    registerActivityLifecycleCallbacks(CrashLogLifecycleCallbacks(this, logger))
                }
            }
    }
}
