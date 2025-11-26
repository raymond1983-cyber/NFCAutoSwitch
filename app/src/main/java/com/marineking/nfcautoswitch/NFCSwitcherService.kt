package com.marineking.nfcautoswitch

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class NfcSwitcherService : AccessibilityService() {

    companion object {
        const val TAG = "NfcSwitcher"
        const val NFC_PAYMENT_DEFAULT_COMPONENT = "nfc_payment_default_component"
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                val screenOffRule = ConfigUtils.getScreenOffRule(applicationContext)
                if (screenOffRule != null) {
                    Log.d(TAG, "Screen Off: Switching to ${screenOffRule.nfcLabel}")
                    changeNfcDefault(screenOffRule.nfcComponent, screenOffRule.nfcLabel)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val currentPackage = event.packageName?.toString() ?: return

            val rules = ConfigUtils.getRules(applicationContext)
            val matchedRule = rules.find { it.packageName == currentPackage }

            if (matchedRule != null) {
                Log.d(TAG, "Detected ${matchedRule.appName}, switching...")
                changeNfcDefault(matchedRule.nfcComponent, matchedRule.nfcLabel)
            }
        }
    }

    private fun changeNfcDefault(targetComponent: String, nfcLabel: String) {
        try {
            val currentSetting = Settings.Secure.getString(contentResolver, NFC_PAYMENT_DEFAULT_COMPONENT)

            if (currentSetting != targetComponent) {
                val needsForceUpdate = ConfigUtils.isRestartNfcEnabled(applicationContext)

                if (needsForceUpdate) {
                    Settings.Secure.putString(contentResolver, NFC_PAYMENT_DEFAULT_COMPONENT, null)
                    Thread {
                        try {
                            Thread.sleep(300)
                            val success = Settings.Secure.putString(contentResolver, NFC_PAYMENT_DEFAULT_COMPONENT, targetComponent)
                            if (success) showToast(getString(R.string.toast_force_switched, nfcLabel))
                        } catch (e: Exception) {
                            Log.e(TAG, "Force update failed", e)
                        }
                    }.start()
                } else {
                    val success = Settings.Secure.putString(contentResolver, NFC_PAYMENT_DEFAULT_COMPONENT, targetComponent)
                    if (success) showToast(getString(R.string.toast_switched, nfcLabel))
                    else Log.e(TAG, "Switch failed (permission?)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error changing NFC", e)
        }
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {}
}