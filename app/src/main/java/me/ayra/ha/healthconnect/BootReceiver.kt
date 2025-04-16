package me.ayra.ha.healthconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the worker immediately on boot
            SyncWorker.startNow(context)

            // Also reschedule the periodic work
            SyncWorker.schedule(context)
        }
    }
}