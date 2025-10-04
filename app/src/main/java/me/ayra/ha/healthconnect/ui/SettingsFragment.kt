package me.ayra.ha.healthconnect.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import me.ayra.ha.healthconnect.ForegroundService
import me.ayra.ha.healthconnect.R
import me.ayra.ha.healthconnect.data.DEFAULT_SYNC_DAYS
import me.ayra.ha.healthconnect.data.MAX_SYNC_DAYS
import me.ayra.ha.healthconnect.data.MIN_SYNC_DAYS
import me.ayra.ha.healthconnect.data.Settings.getForegroundServiceEnabled
import me.ayra.ha.healthconnect.data.Settings.getSettings
import me.ayra.ha.healthconnect.data.Settings.getSyncDays
import me.ayra.ha.healthconnect.data.Settings.setForegroundServiceEnabled
import me.ayra.ha.healthconnect.data.Settings.setSettings
import me.ayra.ha.healthconnect.data.Settings.setSyncDays
import me.ayra.ha.healthconnect.utils.AppUtils.openUrlInBrowser
import me.ayra.ha.healthconnect.utils.UiUtils.navigate

class SettingsFragment : PreferenceFragmentCompat() {
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val context = context ?: return@registerForActivityResult
                context.setForegroundServiceEnabled(true)
                val serviceIntent =
                    Intent(context.applicationContext, ForegroundService::class.java)
                ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
                findPreference<SwitchPreferenceCompat>("foregroundService")?.isChecked = true
            }
        }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        setupIntervalPreference()
        setupSyncDaysPreference()
        setupForegroundServicePreference()
        setupUrlPreference()
        setupTokenPreference()
        setupSensorEntityPreference()
        setupHealthDataPreference()
        setupAboutPreference()
    }

    private fun setupForegroundServicePreference() {
        val preference = findPreference<SwitchPreferenceCompat>("foregroundService") ?: return
        val storedEnabled = context?.getForegroundServiceEnabled() ?: false
        val resolvedEnabled = storedEnabled && hasNotificationPermission()
        preference.isChecked = resolvedEnabled
        if (storedEnabled != resolvedEnabled) {
            context?.setForegroundServiceEnabled(false)
            context?.let { ForegroundService.stopService(it) }
        }
        preference.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as? Boolean ?: return@setOnPreferenceChangeListener false
            val context = context ?: return@setOnPreferenceChangeListener false

            if (enabled) {
                if (!hasNotificationPermission()) {
                    if (requiresNotificationPermission()) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    return@setOnPreferenceChangeListener false
                }

                context.setForegroundServiceEnabled(true)
                val serviceIntent = Intent(context.applicationContext, ForegroundService::class.java)
                ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
            } else {
                context.setForegroundServiceEnabled(false)
                ForegroundService.stopService(context)
            }

            true
        }
    }

    private fun setupIntervalPreference() {
        val intervalPreference = findPreference<Preference>("updateInterval") ?: return
        val labels = resources.getStringArray(R.array.sync_intervals)
        val values = resources.getStringArray(R.array.sync_time)
        val savedValue = context?.getSettings("updateInterval")?.toString() ?: DEFAULT_INTERVAL

        // Set initial summary
        val savedIndex = values.indexOf(savedValue).takeIf { it >= 0 } ?: 0
        intervalPreference.summary = labels.getOrNull(savedIndex) ?: labels[0]

        intervalPreference.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.update_interval_title)
                .setItems(labels) { _, which ->
                    val selectedLabel = labels.getOrNull(which) ?: return@setItems
                    val selectedValue = values.getOrNull(which) ?: return@setItems
                    context?.setSettings("updateInterval", selectedValue)
                    intervalPreference.summary = selectedLabel
                }.setNegativeButton(R.string.cancel, null)
                .show()
            true
        }
    }

    private fun setupSyncDaysPreference() {
        val syncDaysPreference = findPreference<Preference>("syncDays") ?: return
        updateSyncDaysSummary()

        syncDaysPreference.setOnPreferenceClickListener {
            val currentDays = context?.getSyncDays() ?: DEFAULT_SYNC_DAYS
            showInputDialog("syncDays", getString(R.string.sync_days_title), currentDays.toString())
            true
        }
    }

    private fun setupUrlPreference() {
        findPreference<Preference>("haUrl")?.apply {
            summary = context.getSettings("url")
            setOnPreferenceClickListener {
                showInputDialog("url", getString(R.string.login_ha_url), summary?.toString())
                true
            }
        }
    }

    private fun setupTokenPreference() {
        findPreference<Preference>("haToken")?.setOnPreferenceClickListener {
            showInputDialog("token", getString(R.string.login_token), "")
            true
        }
    }

    private fun setupSensorEntityPreference() {
        findPreference<Preference>("sensorEntity")?.apply {
            summary = "sensor.${context.getSettings("sensor")}"
            setOnPreferenceClickListener {
                showInputDialog("sensor", getString(R.string.login_sensor_hint), summary?.toString())
                true
            }
        }
    }

    private fun setupHealthDataPreference() {
        findPreference<Preference>("healthData")?.setOnPreferenceClickListener {
            activity?.navigate(R.id.settings_health_data_fragment)
            true
        }
    }

    private fun updateSyncDaysSummary() {
        val days = context?.getSyncDays() ?: DEFAULT_SYNC_DAYS
        findPreference<Preference>("syncDays")?.summary = getSyncDaysSummary(days)
    }

    private fun getSyncDaysSummary(days: Long): String {
        val quantity = days.toInt().coerceAtLeast(1)
        return resources.getQuantityString(R.plurals.sync_days_summary, quantity, days)
    }

    private fun setupAboutPreference() {
        findPreference<Preference>("app_version")?.apply {
            try {
                val versionName =
                    requireContext()
                        .packageManager
                        .getPackageInfo(requireContext().packageName, 0)
                        .versionName
                summary = getString(R.string.version_format, versionName)
            } catch (e: Exception) {
                summary = getString(R.string.version_unknown)
            }
        }

        findPreference<Preference>("source_code")?.setOnPreferenceClickListener {
            activity?.openUrlInBrowser("https://github.com/AyraHikari/HealthConnect_to_HomeAssistant")
            true
        }
    }

    private fun showInputDialog(
        key: String,
        title: String,
        initialValue: String?,
    ) {
        val context = context ?: return
        isSettingsUpdate = true

        val inputEditText =
            TextInputEditText(context).apply {
                setText(initialValue)
                when (key) {
                    "url" -> {
                        hint = getString(R.string.url_hint)
                        inputType = InputType.TYPE_TEXT_VARIATION_URI
                    }
                    "token" -> {
                        hint = getString(R.string.token_hint)
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }
                    "sensor" -> {
                        hint = getString(R.string.sensor_hint)
                        inputType = InputType.TYPE_CLASS_TEXT
                        filters =
                            arrayOf(
                                InputFilter { source, start, end, dest, dstart, dend ->
                                    val regex = Regex("^[a-zA-Z0-9_]*$")
                                    if (source.isNullOrBlank() || regex.matches(source)) {
                                        null // Accept the input
                                    } else {
                                        "" // Reject the input
                                    }
                                },
                            )
                    }
                    "syncDays" -> {
                        hint = getString(R.string.sync_days_hint)
                        inputType = InputType.TYPE_CLASS_NUMBER
                    }
                }
            }

        val inputLayout =
            TextInputLayout(context).apply {
                addView(inputEditText)
                setPadding(
                    resources.getDimensionPixelOffset(R.dimen.dialog_padding),
                    0,
                    resources.getDimensionPixelOffset(R.dimen.dialog_padding),
                    0,
                )
            }

        val dialog =
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(inputLayout)
                .setPositiveButton(getString(R.string.save), null) // Set to null initially
                .setNegativeButton(getString(R.string.cancel), null)
                .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newValue = inputEditText.text?.toString()?.trim()
                when {
                    newValue.isNullOrEmpty() -> {
                        inputLayout.error = getString(R.string.error_field_required)
                        return@setOnClickListener
                    }
                    key == "sensor" && !newValue.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                        inputLayout.error = getString(R.string.error_invalid_sensor_format)
                        return@setOnClickListener
                    }
                    key == "syncDays" -> {
                        val days = newValue.toLongOrNull()
                        if (days == null) {
                            inputLayout.error = getString(R.string.error_invalid_sync_days, MIN_SYNC_DAYS, MAX_SYNC_DAYS)
                            return@setOnClickListener
                        }

                        if (days !in MIN_SYNC_DAYS..MAX_SYNC_DAYS) {
                            inputLayout.error = getString(R.string.error_invalid_sync_days, MIN_SYNC_DAYS, MAX_SYNC_DAYS)
                            return@setOnClickListener
                        }

                        context.setSyncDays(days)
                        updateSyncDaysSummary()
                        dialog.dismiss()
                    }
                    else -> {
                        context.setSettings(key, newValue)
                        when (key) {
                            "url" -> findPreference<Preference>("haUrl")?.summary = newValue
                            "token" ->
                                findPreference<Preference>("haToken")?.let { pref ->
                                    pref.summary =
                                        if (newValue.length > 4) {
                                            "••••${newValue.takeLast(4)}"
                                        } else {
                                            "••••"
                                        }
                                }
                            "sensor" -> findPreference<Preference>("sensorEntity")?.summary = newValue
                        }
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()

        // Show keyboard automatically
        inputEditText.requestFocus()
        val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
        imm?.showSoftInput(inputEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hasNotificationPermission(): Boolean {
        val context = context ?: return false
        if (!requiresNotificationPermission()) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiresNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    companion object {
        private const val DEFAULT_INTERVAL = "3600"
        var isSettingsUpdate = false
    }
}
