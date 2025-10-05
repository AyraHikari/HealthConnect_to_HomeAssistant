package me.ayra.ha.healthconnect.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import me.ayra.ha.healthconnect.R
import me.ayra.ha.healthconnect.data.Settings.getSettings
import me.ayra.ha.healthconnect.data.Settings.setSettings
import me.ayra.ha.healthconnect.databinding.FragmentLoginBinding
import me.ayra.ha.healthconnect.network.HomeAssistant.checkHomeAssistant
import me.ayra.ha.healthconnect.utils.Coroutines.ioSafe
import me.ayra.ha.healthconnect.utils.Coroutines.runOnMainThread
import me.ayra.ha.healthconnect.utils.UiUtils.navigate
import me.ayra.ha.healthconnect.utils.UiUtils.showError
import me.ayra.ha.healthconnect.utils.UiUtils.showSuccess
import kotlin.concurrent.thread

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val labels = resources.getStringArray(R.array.sync_intervals)
        val values = resources.getStringArray(R.array.sync_time)

        binding.apply {
            updateIntervalHolder.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Update interval")
                    .setItems(labels) { _, which ->
                        val selectedLabel = labels[which]
                        val selectedValue = values[which]
                        updateInterval.setText(selectedLabel)
                        updateInterval.tag = selectedValue
                    }.setNegativeButton("Cancel", null)
                    .show()
            }

            login.setOnClickListener {
                validateAndLogin()
            }
        }
    }

    private fun validateAndLogin() {
        binding.apply {
            val url = haUrl.text.toString().trim()
            val token = haToken.text.toString().trim()
            val sensorName = sensor.text.toString().trim()
            val updateInterval = updateInterval

            when {
                url.isEmpty() -> showError("Please enter Home Assistant URL")
                token.isEmpty() -> showError("Please enter your access token")
                sensorName.isEmpty() -> showError("Please enter a sensor name")
                else -> {
                    setLoading(true)
                    thread {
                        ioSafe {
                            performLogin(url, token, sensorName, updateInterval)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (context?.getSettings("url") != null &&
            context?.getSettings("token") != null &&
            context?.getSettings("sensor") != null
        ) {
            activity?.navigate(R.id.home_fragment, inclusive = true)
        }
    }

    private fun performLogin(
        url: String,
        token: String,
        sensorName: String,
        updateInterval: TextInputEditText,
    ) {
        val (isAuthOk, message) = checkHomeAssistant(url, token)
        runOnMainThread {
            val binding = _binding ?: return@runOnMainThread
            setLoading(false)

            if (isAuthOk) {
                binding.showSuccess("Login successful!")
                context?.setSettings("url", url)
                context?.setSettings("token", token)
                context?.setSettings("sensor", sensorName)
                val updateIntervalSec = updateInterval.tag as? String ?: "900"
                context?.setSettings("updateInterval", updateIntervalSec)
                activity?.navigate(R.id.home_fragment)
            } else {
                binding.showError("Login failed: $message")
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        val binding = _binding ?: return
        binding.login.isEnabled = !isLoading
        binding.loginProgress.isVisible = isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = LoginFragment()
    }
}
