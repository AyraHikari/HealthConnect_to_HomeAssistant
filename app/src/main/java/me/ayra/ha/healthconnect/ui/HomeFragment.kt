package me.ayra.ha.healthconnect.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.health.connect.client.PermissionController
import me.ayra.ha.healthconnect.R
import me.ayra.ha.healthconnect.SyncWorker
import me.ayra.ha.healthconnect.data.Settings
import me.ayra.ha.healthconnect.data.Settings.getAutoSync
import me.ayra.ha.healthconnect.data.Settings.getLastError
import me.ayra.ha.healthconnect.data.Settings.getLastSync
import me.ayra.ha.healthconnect.data.Settings.getSettings
import me.ayra.ha.healthconnect.data.Settings.removeLastError
import me.ayra.ha.healthconnect.data.Settings.setAutoSync
import me.ayra.ha.healthconnect.data.Settings.setLastError
import me.ayra.ha.healthconnect.data.Settings.setLastSync
import me.ayra.ha.healthconnect.data.Settings.setSettings
import me.ayra.ha.healthconnect.databinding.FragmentHomeBinding
import me.ayra.ha.healthconnect.network.HomeAssistant.sendToHomeAssistant
import me.ayra.ha.healthconnect.ui.SettingsFragment.Companion.isSettingsUpdate
import me.ayra.ha.healthconnect.utils.Coroutines.ioSafe
import me.ayra.ha.healthconnect.utils.Coroutines.main
import me.ayra.ha.healthconnect.utils.DataStore.toJson
import me.ayra.ha.healthconnect.utils.HealthConnectManager
import me.ayra.ha.healthconnect.utils.HealthData
import me.ayra.ha.healthconnect.utils.TimeUtils.toDate
import me.ayra.ha.healthconnect.utils.TimeUtils.unixTimeMs
import me.ayra.ha.healthconnect.utils.UiUtils.navigate
import me.ayra.ha.healthconnect.utils.UiUtils.showError
import me.ayra.ha.healthconnect.utils.UiUtils.showSuccess
import me.ayra.ha.healthconnect.utils.UiUtils.startRotate
import me.ayra.ha.healthconnect.utils.UiUtils.stopRotate
import me.ayra.ha.healthconnect.utils.healthConnectPermissions
import kotlin.concurrent.thread

class HomeFragment : Fragment() {
    private lateinit var backCallback: OnBackPressedCallback
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var hc: HealthConnectManager

    private val requestPermissions =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
        ) { granted ->
            if (granted.containsAll(healthConnectPermissions)) {
                // All permissions granted
            } else {
                // Some permissions denied
            }
            checkHcPermission()
            checkLastError()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        backCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (shouldInterceptBackPress()) {
                        requireActivity().moveTaskToBack(true)
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }

        requireActivity().onBackPressedDispatcher.addCallback(this, backCallback)

        hc = HealthConnectManager(requireContext())
        val health = HealthData(requireContext())

        binding.apply {
            hcGrantPermission.setOnClickListener {
                requestPermissions.launch(healthConnectPermissions)
            }

            val lastSyncSaved = context?.getLastSync() ?: 0
            lastSync.text =
                getString(R.string.last_sync).format(
                    if (lastSyncSaved <=
                        0
                    ) {
                        getString(R.string.never)
                    } else {
                        lastSyncSaved.toDate("dd/MM/yyyy HH.mm")
                    },
                )

            val labels = resources.getStringArray(R.array.sync_intervals)
            val values = resources.getStringArray(R.array.sync_time)

            val savedValue = context?.getSettings("updateInterval")?.toString() ?: "3600"
            val index = values.indexOf(savedValue)

            val label = if (index != -1) labels[index] else "Unknown"
            autoSync.apply {
                text = getString(R.string.auto_sync).format(label)
                isChecked = context?.getAutoSync() == true
                setOnCheckedChangeListener { _, isChecked ->
                    context?.setAutoSync(isChecked)
                }
            }

            manualSync.setOnClickListener {
                sync.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_sync_24px))
                startRotate(sync)
                thread {
                    ioSafe {
                        val permission = hc.hasAllPermissions()
                        if (!permission) {
                            main {
                                showError(getString(R.string.hc_permission_denied))
                                stopRotate(sync)
                                sync.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_error_24px))
                            }
                            return@ioSafe
                        }
                        val healthData = health.getHealthData(hc)
                        context?.setSettings("health_data", healthData.toJson())
                        val (isSuccess, message) =
                            sendToHomeAssistant(
                                healthData,
                                entityId = context?.getSettings("sensor") ?: "health_connect",
                                apiUrl = context?.getSettings("url") ?: return@ioSafe,
                                apiToken = context?.getSettings("token") ?: return@ioSafe,
                            )
                        main {
                            stopRotate(sync)
                            if (isSuccess) {
                                context?.setLastSync(unixTimeMs)
                                errorMessage.visibility = View.GONE
                                sync.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_downward_24px))
                                lastSync.text =
                                    getString(R.string.last_sync).format((context?.getLastSync() ?: 0).toDate("dd/MM/yyyy HH.mm"))
                                showSuccess(getString(R.string.sync_success))
                                context?.removeLastError()
                            } else {
                                sync.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_error_24px))
                                errorMessage.apply {
                                    text = getString(R.string.error_time).format(unixTimeMs.toDate("dd/MM/yyyy HH.mm"), message)
                                    visibility = View.VISIBLE
                                }
                                showError(getString(R.string.ha_error).format(message))
                                context?.setLastError(Settings.SyncError(unixTimeMs, message))
                            }
                            checkLastError()
                        }
                    }
                }
            }

            settings.setOnClickListener {
                activity?.navigate(R.id.settings_fragment)
            }
        }

        checkHcPermission()
        checkLastError()
    }

    override fun onResume() {
        super.onResume()
        SyncWorker.startNow(requireContext())
        if (isSettingsUpdate) {
            SyncWorker.schedule(requireContext())
            isSettingsUpdate = false
        }
        checkHcPermission()
        checkLastError()
    }

    private fun checkLastError() {
        val lastError = context?.getLastError()
        if (lastError != null) {
            main {
                binding.apply {
                    sync.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_error_24px))
                    errorMessage.apply {
                        text = getString(R.string.error_time).format(unixTimeMs.toDate("dd/MM/yyyy HH.mm"), lastError.message)
                        visibility = View.VISIBLE
                    }
                    haError.isVisible = true
                    haErrorMessage.text = lastError.message
                    haValid.isVisible = false
                }
            }
        } else {
            main {
                binding.haError.isVisible = false
                binding.haValid.isVisible = true
            }
        }
    }

    @SuppressLint("ResourceType")
    private fun checkHcPermission() {
        thread {
            ioSafe {
                val permission = hc.hasAllPermissions()
                main {
                    if (!permission) {
                        binding.apply {
                            hcGrantPermission.isVisible = true
                            hcPermissionError.isVisible = true
                            hcPermissionOk.isVisible = false
                        }
                    } else {
                        binding.apply {
                            hcGrantPermission.isVisible = false
                            hcPermissionError.isVisible = false
                            hcPermissionOk.isVisible = true
                        }
                    }
                }
            }
        }
    }

    private fun shouldInterceptBackPress(): Boolean {
        // Add your logic here
        return true // Return true to intercept, false to allow default behavior
    }

    override fun onDestroy() {
        super.onDestroy()
        backCallback.remove()
    }
}
