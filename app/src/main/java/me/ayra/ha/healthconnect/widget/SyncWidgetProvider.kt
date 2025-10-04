package me.ayra.ha.healthconnect.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import me.ayra.ha.healthconnect.R
import me.ayra.ha.healthconnect.SyncWorker
import me.ayra.ha.healthconnect.data.Settings.getLastSync
import me.ayra.ha.healthconnect.utils.TimeUtils.toDate
import java.util.concurrent.TimeUnit

class SyncWidgetProvider : AppWidgetProvider() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SYNC_NOW) {
            showSyncingState(context)
            SyncWorker.startNow(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val ACTION_SYNC_NOW = "me.ayra.ha.healthconnect.widget.ACTION_SYNC_NOW"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SyncWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_sync)

            val lastSyncTime = context.getLastSync() ?: 0L
            val formattedTime =
                if (lastSyncTime <= 0L) {
                    context.getString(R.string.never)
                } else {
                    val now = System.currentTimeMillis()
                    val diff = now - lastSyncTime
                    val dayMillis = TimeUnit.DAYS.toMillis(1)
                    when {
                        diff < dayMillis -> lastSyncTime.toDate("HH:mm")
                        diff < TimeUnit.DAYS.toMillis(2) ->
                            context.getString(R.string.widget_last_sync_one_day_ago)
                        diff < TimeUnit.DAYS.toMillis(14) -> {
                            val days = (diff / dayMillis).toInt()
                            context.resources.getQuantityString(
                                R.plurals.widget_last_sync_days_ago,
                                days,
                                days,
                            )
                        }
                        else -> context.getString(R.string.widget_last_sync_more_than_weeks_ago)
                    }
                }
            views.setTextViewText(
                R.id.widget_last_sync,
                context.getString(R.string.widget_last_sync, formattedTime),
            )
            views.setOnClickPendingIntent(
                R.id.widget_sync_button,
                createSyncPendingIntent(context),
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createSyncPendingIntent(context: Context): PendingIntent {
            val intent =
                Intent(context, SyncWidgetProvider::class.java).apply {
                    action = ACTION_SYNC_NOW
                }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun showSyncingState(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SyncWidgetProvider::class.java)
            val views = RemoteViews(context.packageName, R.layout.widget_sync)
            views.setTextViewText(R.id.widget_last_sync, context.getString(R.string.widget_syncing))
            views.setOnClickPendingIntent(R.id.widget_sync_button, createSyncPendingIntent(context))
            appWidgetManager.updateAppWidget(componentName, views)
        }
    }
}
