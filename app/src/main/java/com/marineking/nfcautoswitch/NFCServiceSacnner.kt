package com.marineking.nfcautoswitch // 修正 package

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class NfcServiceInfo(
    val label: String,
    val componentStr: String,
    val triggerPackage: String
)

object NfcServiceScanner {

    fun scan(context: Context): List<NfcServiceInfo> {
        val pm = context.packageManager
        val nfcList = mutableListOf<NfcServiceInfo>()
        val seenComponents = mutableSetOf<String>() // 用來去重

        // 定義我們要掃描的兩種 Action
        val actions = listOf(
            "android.nfc.cardemulation.action.HOST_APDU_SERVICE",      // HCE (Google Pay 等)
            "android.nfc.cardemulation.action.OFF_HOST_APDU_SERVICE"   // SIM/eSE (悠遊付、電信卡等)
        )

        for (action in actions) {
            val intent = Intent(action)
            // 加上 GET_RESOLVED_FILTER 以確保更廣泛的相容性
            val resolvedServices = pm.queryIntentServices(intent, PackageManager.GET_META_DATA or PackageManager.GET_RESOLVED_FILTER)

            for (resolveInfo in resolvedServices) {
                try {
                    val serviceInfo = resolveInfo.serviceInfo
                    val packageName = serviceInfo.packageName
                    val className = serviceInfo.name
                    val componentStr = "$packageName/$className"

                    // 避免重複加入 (有些 App 可能同時註冊兩種，或被多次掃描到)
                    if (seenComponents.contains(componentStr)) continue
                    seenComponents.add(componentStr)

                    var triggerPkg = packageName

                    // [特例處理] Google Pay
                    if (packageName == "com.google.android.gms") {
                        triggerPkg = "com.google.android.apps.walletnfcrel"
                    }

                    // 嘗試取得 App 顯示名稱
                    var label = resolveInfo.loadLabel(pm).toString()
                    try {
                        // 嘗試抓取觸發 App 的名稱 (例如抓取 "Google 錢包" 而不是 "Google Play 服務")
                        val appInfo = pm.getApplicationInfo(triggerPkg, 0)
                        label = pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        // 找不到觸發 App，維持原 Service Label
                    }

                    nfcList.add(NfcServiceInfo(label, componentStr, triggerPkg))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return nfcList.sortedBy { it.label }
    }
}