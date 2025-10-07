package me.ayra.ha.healthconnect

import android.app.Application
import com.google.android.material.color.DynamicColors

class HealthConnectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
