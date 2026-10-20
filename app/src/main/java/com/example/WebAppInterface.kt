package com.example

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(
    private val activity: Activity,
    private val onCarDeliveredCallback: (Int) -> Unit,
    private val onStormTriggeredCallback: () -> Unit
) {
    @JavascriptInterface
    fun onCarDelivered(doneCount: Int) {
        activity.runOnUiThread {
            vibrate(50)
            onCarDeliveredCallback(doneCount)
        }
    }

    @JavascriptInterface
    fun onStormTriggered() {
        activity.runOnUiThread {
            vibrate(100)
            onStormTriggeredCallback()
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            @Suppress("DEPRECATION")
            val vibrator = activity.getSystemService(Activity.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
        }
    }
}
