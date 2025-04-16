package me.ayra.ha.healthconnect.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import me.ayra.ha.healthconnect.R

object AppUtils {
    fun Activity.openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                getString(R.string.error_no_browser),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.error_open_url),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}