package me.ayra.ha.healthconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.ayra.ha.healthconnect.ForegroundService.Companion.runServiceIfEnabled
import me.ayra.ha.healthconnect.data.Settings.isNotificationPromptDisabled
import me.ayra.ha.healthconnect.data.Settings.setNotificationPromptDisabled
import me.ayra.ha.healthconnect.databinding.ActivityMainBinding
import me.ayra.ha.healthconnect.network.initializeGlideWithUnsafeOkHttp
import me.ayra.ha.healthconnect.utils.Coroutines.ioSafe
import me.ayra.ha.healthconnect.utils.HealthConnectManager
import me.ayra.ha.healthconnect.utils.healthConnectPermissions
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var hc: HealthConnectManager

    private val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
    private val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(healthConnectPermissions)) {
                // Permissions successfully granted
            } else {
                requestPermissionLauncher.launch(healthConnectPermissions.first())
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                // Optionally guide user to open Health Connect manually
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                applicationContext.setNotificationPromptDisabled(false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleSyncWorker()
        applicationContext.runServiceIfEnabled()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeGlideWithUnsafeOkHttp(this)

        val navHostFragment =
            supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        hc = HealthConnectManager(this)

        promptForNotificationPermissionIfNeeded()

        thread {
            ioSafe {
                checkAndRequestPermissions()
            }
        }
    }

    private suspend fun checkAndRequestPermissions() {
        if (hc.hasAllPermissions()) {
            requestPermissions.launch(healthConnectPermissions)
        }
    }

    private fun promptForNotificationPermissionIfNeeded() {
        if (!requiresNotificationPermission()) return
        if (applicationContext.isNotificationPromptDisabled()) return
        if (hasNotificationPermission()) return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setPositiveButton(R.string.allow) { _, _ ->
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }.setNegativeButton(R.string.deny, null)
            .setNeutralButton(R.string.never_show_again) { _, _ ->
                applicationContext.setNotificationPromptDisabled(true)
            }.show()
    }

    private fun hasNotificationPermission(): Boolean {
        if (!requiresNotificationPermission()) return true

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiresNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun scheduleSyncWorker() {
        SyncWorker.schedule(applicationContext)
    }
}
