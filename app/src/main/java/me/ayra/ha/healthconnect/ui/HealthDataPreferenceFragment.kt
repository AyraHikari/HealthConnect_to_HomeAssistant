package me.ayra.ha.healthconnect.ui

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import me.ayra.ha.healthconnect.R
import me.ayra.ha.healthconnect.data.Settings.getSettings
import me.ayra.ha.healthconnect.data.Settings.setSettings

class HealthDataPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.health_data_pref, rootKey)
        initializePreferenceStates()
    }

    private fun initializePreferenceStates() {
        val preferenceKeys = listOf(
            "sleep",
            "heartRate",
            "steps",
            "weight",
            "bodyTemperature",
            "exercise",
            "oxygen",
            "hydration",
            "calories"
        )

        preferenceKeys.forEach { key ->
            findPreference<CheckBoxPreference>(key)?.let { preference ->
                preference.isChecked = context?.getSettings(key, true) == true

                preference.setOnPreferenceChangeListener { _, value ->
                    value as Boolean
                    context?.setSettings(key, value)
                    true
                }
            }
        }
    }
}