package me.ayra.ha.healthconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
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
    private var bottomNavHeight = 0

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

        binding.bottomNav.doOnLayout {
            bottomNavHeight = it.height
        }

        val navHostFragment =
            supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (navController.currentDestination?.id != R.id.home_fragment) {
                        navController.navigate(R.id.home_fragment)
                    }
                    true
                }

                R.id.navigation_settings -> {
                    if (navController.currentDestination?.id != R.id.settings_fragment) {
                        navController.navigate(R.id.settings_fragment)
                    }
                    true
                }

                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav =
                destination.id == R.id.home_fragment || destination.id == R.id.settings_fragment
            binding.bottomNav.animate().cancel()

            if (showBottomNav) {
                val navHeight = bottomNavHeight.takeIf { it != 0 } ?: binding.bottomNav.height
                if (!binding.bottomNav.isVisible) {
                    binding.bottomNav.translationY = navHeight.toFloat()
                    binding.bottomNav.alpha = 0f
                    binding.bottomNav.isVisible = true
                }

                binding.bottomNav
                    .animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .start()

                binding.bottomNav.selectedItemId =
                    if (destination.id == R.id.home_fragment) {
                        R.id.navigation_home
                    } else {
                        R.id.navigation_settings
                    }
            } else if (binding.bottomNav.isVisible) {
                val navHeight = bottomNavHeight.takeIf { it != 0 } ?: binding.bottomNav.height
                binding.bottomNav
                    .animate()
                    .translationY(navHeight.toFloat())
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        binding.bottomNav.isVisible = false
                    }.start()
            }
        }

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
