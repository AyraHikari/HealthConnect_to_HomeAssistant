package me.ayra.ha.healthconnect.utils

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.ayra.ha.healthconnect.R
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.roundToInt
import kotlin.system.exitProcess

private const val TAG = "CrashLogger"

class CrashLogger(
    context: Context,
) : Thread.UncaughtExceptionHandler {
    private val appContext = context.applicationContext
    private val preferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    fun hasPendingCrashLog(): Boolean = preferences.contains(KEY_CRASH_LOG)

    fun consumeCrashLog(): String? {
        val crashLog = preferences.getString(KEY_CRASH_LOG, null)
        if (crashLog != null) {
            preferences.edit().remove(KEY_CRASH_LOG).apply()
        }
        return crashLog
    }

    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        runCatching {
            val stackTrace = buildCrashLog(thread, throwable)
            preferences.edit().putString(KEY_CRASH_LOG, stackTrace).apply()
            Log.e(TAG, "Uncaught exception captured", throwable)
        }

        val handler = defaultHandler
        if (handler != null) {
            handler.uncaughtException(thread, throwable)
        } else {
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun buildCrashLog(
        thread: Thread,
        throwable: Throwable,
    ): String {
        val stringWriter = StringWriter()
        PrintWriter(stringWriter).use { writer ->
            writer.append("Thread: ").append(thread.name).append('\n')
            writer.append("Exception: ").append(throwable::class.java.name).append('\n')
            writer.append("Message: ").append(throwable.message ?: "").append('\n')
            writer.appendLine()
            throwable.printStackTrace(writer)
        }
        return stringWriter.toString()
    }

    companion object {
        private const val PREFS_NAME = "crash_logs"
        private const val KEY_CRASH_LOG = "last_crash"
    }
}

class CrashLogLifecycleCallbacks(
    private val application: Application,
    private val crashLogger: CrashLogger,
) : Application.ActivityLifecycleCallbacks {
    private var hasShownDialog = false

    override fun onActivityResumed(activity: Activity) {
        if (hasShownDialog) return

        val crashLog = crashLogger.consumeCrashLog()?.takeIf { it.isNotBlank() } ?: return
        hasShownDialog = true
        showCrashDialog(activity, crashLog)
        application.unregisterActivityLifecycleCallbacks(this)
    }

    private fun showCrashDialog(
        activity: Activity,
        crashLog: String,
    ) {
        val padding = (16 * activity.resources.displayMetrics.density).roundToInt()

        val container =
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
            }

        val messageView =
            TextView(activity).apply {
                text = activity.getString(R.string.crash_dialog_message)
                setPadding(0, 0, 0, padding / 2)
            }

        val logView =
            TextView(activity).apply {
                text = crashLog
                typeface = android.graphics.Typeface.MONOSPACE
                setTextIsSelectable(true)
            }

        val scrollView =
            ScrollView(activity).apply {
                setPadding(0, padding / 2, 0, 0)
                isFillViewport = true
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (240 * activity.resources.displayMetrics.density).roundToInt(),
                    )
                addView(logView)
            }

        container.addView(messageView)
        container.addView(scrollView)

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.crash_dialog_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.crash_dialog_copy) { _, _ ->
                copyCrashLogToClipboard(activity, crashLog)
            }.show()
    }

    private fun copyCrashLogToClipboard(
        activity: Activity,
        crashLog: String,
    ) {
        val clipboard = activity.getSystemService<android.content.ClipboardManager>()
        clipboard?.setPrimaryClip(
            ClipData.newPlainText(
                activity.getString(R.string.crash_dialog_clipboard_label),
                crashLog,
            ),
        )
        Toast.makeText(activity, R.string.crash_dialog_copied, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
