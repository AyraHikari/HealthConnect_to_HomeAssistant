package me.ayra.ha.healthconnect.utils

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Spanned
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import me.ayra.ha.healthconnect.R

object UiUtils {
    fun Activity?.popBackStack() {
        try {
            if (this is FragmentActivity) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment?
                navHostFragment?.navController?.popBackStack()
            }
        } catch (t: Throwable) {
            Log.e("UI", t.message.toString())
        }
    }

    fun Activity?.navigate(@IdRes navigation: Int, arguments: Bundle? = null, navOpt: NavOptions? = null, inclusive: Boolean = false) {
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
        try {
            if (this is FragmentActivity) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment?
                navHostFragment?.navController?.let {
                    if (inclusive) it.navigateUp()
                    it.navigate(navigation, arguments, navOpt ?: navOptions)
                }
            }
        } catch (t: Throwable) {
            Log.e("UI", t.message.toString())
        }
    }

    fun ViewBinding.showSnackBar(message: String) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    fun ViewBinding.showError(message: String) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG)
        val context = root.context
        snackbar.setBackgroundTint(context.getColor(android.R.color.holo_red_dark))
        snackbar.show()
    }

    fun ViewBinding.showSuccess(message: String) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG)
        val context = root.context
        snackbar.setBackgroundTint(context.getColor(android.R.color.holo_green_dark))
        snackbar.show()
    }

    fun alertPopup(context: Context, title: String, description: String, shouldExit: Boolean = false) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(title)
        builder.setMessage(description)
        builder.apply {
            setPositiveButton("Oke") { _, _ ->
                if (shouldExit) android.os.Process.killProcess(android.os.Process.myPid())
            }
            setOnDismissListener {
                if (shouldExit) android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
        builder.show()
    }

    fun alertPopup(context: Context, title: String, description: Spanned, shouldExit: Boolean = false) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(title)
        builder.setMessage(description)
        builder.apply {
            setPositiveButton("Oke") { _, _ ->
                if (shouldExit) android.os.Process.killProcess(android.os.Process.myPid())
            }
            setOnDismissListener {
                if (shouldExit) android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
        builder.show()
    }

    fun startRotate(imageView: ImageView) {
        val rotate = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        rotate.start()
        imageView.tag = rotate
    }

    fun stopRotate(imageView: ImageView) {
        (imageView.tag as? ObjectAnimator)?.cancel()

        val currentRotation = imageView.rotation % 360
        val stopRotation = ObjectAnimator.ofFloat(imageView, "rotation", currentRotation, 0f).apply {
            duration = 300
            interpolator = LinearInterpolator()
        }
        stopRotation.start()
    }
}