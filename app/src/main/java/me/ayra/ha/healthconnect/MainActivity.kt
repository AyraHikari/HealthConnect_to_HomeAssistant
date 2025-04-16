package me.ayra.ha.healthconnect

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.PermissionController
import androidx.navigation.fragment.NavHostFragment
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
    private val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
        if (granted.containsAll(healthConnectPermissions)) {
            // Permissions successfully granted
        } else {
            requestPermissionLauncher.launch(healthConnectPermissions.first())
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            // Optionally guide user to open Health Connect manually
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleSyncWorker()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeGlideWithUnsafeOkHttp(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        hc = HealthConnectManager(this)

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

    fun scheduleSyncWorker() {
        SyncWorker.schedule(applicationContext)
    }
}
