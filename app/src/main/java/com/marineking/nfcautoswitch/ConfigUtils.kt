package com.marineking.nfcautoswitch

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class NfcRule(
    val appName: String,
    val packageName: String,
    val nfcComponent: String,
    val nfcLabel: String
)

object ConfigUtils {
    private const val PREF_NAME = "nfc_switch_rules"
    private const val KEY_RULES = "rules"
    private const val KEY_SCREEN_OFF_RULE = "screen_off_rule"
    private const val KEY_RESTART_NFC = "restart_nfc_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveRules(context: Context, rules: List<NfcRule>) {
        val jsonArray = JSONArray()
        for (rule in rules) {
            val jsonObj = JSONObject().apply {
                put("appName", rule.appName)
                put("packageName", rule.packageName)
                put("nfcComponent", rule.nfcComponent)
                put("nfcLabel", rule.nfcLabel)
            }
            jsonArray.put(jsonObj)
        }
        getPrefs(context).edit().putString(KEY_RULES, jsonArray.toString()).apply()
    }

    fun getRules(context: Context): MutableList<NfcRule> {
        val jsonString = getPrefs(context).getString(KEY_RULES, "[]")
        val rules = mutableListOf<NfcRule>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                rules.add(NfcRule(
                    obj.getString("appName"),
                    obj.getString("packageName"),
                    obj.getString("nfcComponent"),
                    obj.getString("nfcLabel")
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return rules
    }

    fun saveScreenOffRule(context: Context, rule: NfcRule?) {
        val editor = getPrefs(context).edit()
        if (rule == null) {
            editor.remove(KEY_SCREEN_OFF_RULE)
        } else {
            val jsonObj = JSONObject().apply {
                put("appName", rule.appName)
                put("packageName", rule.packageName)
                put("nfcComponent", rule.nfcComponent)
                put("nfcLabel", rule.nfcLabel)
            }
            editor.putString(KEY_SCREEN_OFF_RULE, jsonObj.toString())
        }
        editor.apply()
    }

    fun getScreenOffRule(context: Context): NfcRule? {
        val jsonString = getPrefs(context).getString(KEY_SCREEN_OFF_RULE, null) ?: return null
        return try {
            val obj = JSONObject(jsonString)
            NfcRule(obj.getString("appName"), obj.getString("packageName"), obj.getString("nfcComponent"), obj.getString("nfcLabel"))
        } catch (e: Exception) { null }
    }

    fun setRestartNfcEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_RESTART_NFC, enabled).apply()
    }

    fun isRestartNfcEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_RESTART_NFC, false)
    }
}