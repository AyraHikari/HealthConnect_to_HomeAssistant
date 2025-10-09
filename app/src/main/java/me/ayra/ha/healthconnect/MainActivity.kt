package me.ayra.ha.healthconnect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ayra.ha.healthconnect.ForegroundService.Companion.runServiceIfEnabled
import me.ayra.ha.healthconnect.data.Settings.getIgnoredUpdateVersion
import me.ayra.ha.healthconnect.data.Settings.isNotificationPromptDisabled
import me.ayra.ha.healthconnect.data.Settings.setIgnoredUpdateVersion
import me.ayra.ha.healthconnect.data.Settings.setNotificationPromptDisabled
import me.ayra.ha.healthconnect.databinding.ActivityMainBinding
import me.ayra.ha.healthconnect.network.initializeGlideWithUnsafeOkHttp
import me.ayra.ha.healthconnect.utils.AppUtils.openUrlInBrowser
import me.ayra.ha.healthconnect.utils.Coroutines.ioSafe
import me.ayra.ha.healthconnect.utils.CrashLogger
import me.ayra.ha.healthconnect.utils.HealthConnectManager
import me.ayra.ha.healthconnect.utils.healthConnectBackgroundPermission
import me.ayra.ha.healthconnect.utils.healthConnectPermissions
import me.ayra.ha.healthconnect.utils.healthConnectReadPermissions
import me.ayra.ha.healthconnect.utils.showCrashLogDialog
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var hc: HealthConnectManager
    private var bottomNavHeight = 0
    private var lastDestinationId: Int? = null
    private val showInterpolator by lazy { OvershootInterpolator() }
    private val defaultInterpolator by lazy { DecelerateInterpolator() }
    private val bottomNavigationAnimationOptions by lazy {
        navOptions {
            anim {
                enter = R.anim.nav_fade_in
                exit = R.anim.nav_fade_out
                popEnter = R.anim.nav_fade_in
                popExit = R.anim.nav_fade_out
            }
            launchSingleTop = true
        }
    }

    private val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
    private val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            val missingReadPermissions = healthConnectReadPermissions - granted

            if (missingReadPermissions.isEmpty() &&
                !granted.contains(healthConnectBackgroundPermission)
            ) {
                requestPermissionLauncher.launch(healthConnectBackgroundPermission)
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

    private val updateClient by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleSyncWorker()
        applicationContext.runServiceIfEnabled()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeGlideWithUnsafeOkHttp(this)

        showPendingCrashLogIfAvailable()

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
                        navController.navigate(
                            R.id.home_fragment,
                            null,
                            bottomNavigationAnimationOptions,
                        )
                    }
                    true
                }

                R.id.navigation_stats -> {
                    if (navController.currentDestination?.id != R.id.stats_fragment) {
                        navController.navigate(
                            R.id.stats_fragment,
                            null,
                            bottomNavigationAnimationOptions,
                        )
                    }
                    true
                }

                R.id.navigation_settings -> {
                    if (navController.currentDestination?.id != R.id.settings_fragment) {
                        navController.navigate(
                            R.id.settings_fragment,
                            null,
                            bottomNavigationAnimationOptions,
                        )
                    }
                    true
                }

                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val destinationsWithBottomNav =
                setOf(R.id.home_fragment, R.id.settings_fragment, R.id.stats_fragment)
            val showBottomNav = destination.id in destinationsWithBottomNav
            val comingFromSetup = lastDestinationId == R.id.login_fragment && destination.id == R.id.home_fragment
            binding.bottomNav.animate().cancel()

            if (showBottomNav) {
                val navHeight = bottomNavHeight.takeIf { it != 0 } ?: binding.bottomNav.height
                if (!binding.bottomNav.isVisible) {
                    binding.bottomNav.translationY = navHeight.toFloat()
                    binding.bottomNav.alpha = 0f
                    if (comingFromSetup) {
                        binding.bottomNav.scaleX = 0.9f
                        binding.bottomNav.scaleY = 0.9f
                    } else {
                        binding.bottomNav.scaleX = 1f
                        binding.bottomNav.scaleY = 1f
                    }
                    binding.bottomNav.isVisible = true
                }

                binding.bottomNav
                    .animate()
                    .translationY(0f)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(if (comingFromSetup) 450 else 300)
                    .setInterpolator(if (comingFromSetup) showInterpolator else defaultInterpolator)
                    .start()

                binding.bottomNav.selectedItemId =
                    when (destination.id) {
                        R.id.home_fragment -> R.id.navigation_home
                        R.id.settings_fragment -> R.id.navigation_settings
                        R.id.stats_fragment -> R.id.navigation_stats
                        else -> binding.bottomNav.selectedItemId
                    }
            } else if (binding.bottomNav.isVisible) {
                val navHeight = bottomNavHeight.takeIf { it != 0 } ?: binding.bottomNav.height
                binding.bottomNav
                    .animate()
                    .translationY(navHeight.toFloat())
                    .alpha(0f)
                    .setInterpolator(defaultInterpolator)
                    .setDuration(300)
                    .withEndAction {
                        binding.bottomNav.isVisible = false
                    }.start()
            }

            lastDestinationId = destination.id
        }

        hc = HealthConnectManager(this)

        promptForNotificationPermissionIfNeeded()
        checkForUpdates()

        thread {
            ioSafe {
                checkAndRequestPermissions()
            }
        }
    }

    private fun checkForUpdates() {
        if (applicationContext.getIgnoredUpdateVersion() == BuildConfig.VERSION_CODE) return

        lifecycleScope.launch {
            val shouldPromptForUpdate =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val request =
                            Request
                                .Builder()
                                .url("https://ayra.eu.org/project/updater/hc?version=${BuildConfig.VERSION_CODE}")
                                .get()
                                .build()
                        updateClient
                            .newCall(request)
                            .execute()
                            .use { response ->
                                if (!response.isSuccessful) return@use false
                                response.body
                                    ?.string()
                                    ?.trim()
                                    ?.equals("true", ignoreCase = true) == true
                            }
                    }.getOrDefault(false)
                }

            if (shouldPromptForUpdate && !isFinishing && !isDestroyed) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.update_available_title)
                    .setMessage(R.string.update_available_message)
                    .setPositiveButton(R.string.update_available_positive) { _, _ ->
                        openUrlInBrowser("https://github.com/AyraHikari/HealthConnect_to_HomeAssistant/releases/latest")
                    }.setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.update_available_neutral) { _, _ ->
                        applicationContext.setIgnoredUpdateVersion(BuildConfig.VERSION_CODE)
                    }.show()
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

    private fun showPendingCrashLogIfAvailable() {
        val preferences = getSharedPreferences(CrashLogger.PREFS_NAME, Context.MODE_PRIVATE)
        val crashLog =
            preferences
                .getString(CrashLogger.KEY_CRASH_LOG, null)
                ?.takeIf { it.isNotBlank() }
                ?: return
        preferences.edit { remove(CrashLogger.KEY_CRASH_LOG) }
        showCrashLogDialog(crashLog)
    }
}
