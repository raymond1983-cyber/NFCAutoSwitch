package com.marineking.nfcautoswitch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var viewDashboard: View
    private lateinit var viewRules: View

    // Dashboard UI Elements
    private lateinit var cardAdb: CardView
    private lateinit var tvAdbStatus: TextView
    private lateinit var imgAdbIcon: ImageView
    private lateinit var cardAccess: CardView
    private lateinit var tvAccessStatus: TextView
    private lateinit var imgAccessIcon: ImageView
    private lateinit var tvCurrentNfcStatus: TextView
    private lateinit var btnSetScreenOff: View
    private lateinit var tvScreenOffDesc: TextView

    // [修正] 這裡原本是 CheckBox，必須改為 SwitchMaterial 以符合 XML 設定
    private lateinit var cbRestartNfc: SwitchMaterial

    // Rules UI Elements
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NfcAppsAdapter
    private lateinit var tvRuleCurrentName: TextView
    private lateinit var tvRuleCurrentPkg: TextView
    private lateinit var tvRuleCurrentComp: TextView
    private lateinit var layoutRuleInfoMain: View
    private lateinit var layoutRuleInfoDetails: View
    private lateinit var imgRuleInfoExpand: ImageView

    data class AppUiModel(val serviceInfo: NfcServiceInfo, var isEnabled: Boolean, var isExpanded: Boolean = false)
    private var appList = mutableListOf<AppUiModel>()
    private var isAdbGranted = false
    private var isAccessEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.fragment_container)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val inflater = LayoutInflater.from(this)

        viewDashboard = inflater.inflate(R.layout.layout_dashboard, container, false)
        viewRules = inflater.inflate(R.layout.layout_rules, container, false)

        container.addView(viewRules)
        container.addView(viewDashboard)
        viewRules.visibility = View.GONE

        initDashboardViews()
        initRulesViews()

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    viewDashboard.visibility = View.VISIBLE
                    viewRules.visibility = View.GONE
                    refreshDashboard()
                    true
                }
                R.id.nav_rules -> {
                    viewDashboard.visibility = View.GONE
                    viewRules.visibility = View.VISIBLE
                    refreshRulesList()
                    refreshRulesInfo()
                    true
                }
                else -> false
            }
        }
    }

    private fun initDashboardViews() {
        cardAdb = viewDashboard.findViewById(R.id.cardAdbStatus)
        tvAdbStatus = viewDashboard.findViewById(R.id.tvAdbStatus)
        imgAdbIcon = viewDashboard.findViewById(R.id.imgAdbIcon)
        cardAccess = viewDashboard.findViewById(R.id.cardAccessStatus)
        tvAccessStatus = viewDashboard.findViewById(R.id.tvAccessStatus)
        imgAccessIcon = viewDashboard.findViewById(R.id.imgAccessIcon)
        tvCurrentNfcStatus = viewDashboard.findViewById(R.id.tvCurrentNfcStatus)
        btnSetScreenOff = viewDashboard.findViewById(R.id.btnSetScreenOff)
        tvScreenOffDesc = viewDashboard.findViewById(R.id.tvScreenOffDesc)

        // 這裡會成功，因為 xml 中 id 為 cbRestartNfc 的元件確實是 SwitchMaterial
        cbRestartNfc = viewDashboard.findViewById(R.id.cbRestartNfc)

        cardAdb.setOnClickListener {
            if (!isAdbGranted) showAdbHelpDialog()
            else Toast.makeText(this, getString(R.string.toast_adb_ok), Toast.LENGTH_SHORT).show()
        }
        cardAccess.setOnClickListener {
            if (!isAccessEnabled) showAccessHelpDialog()
            else startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btnSetScreenOff.setOnClickListener { showScreenOffDialog() }
        cbRestartNfc.setOnCheckedChangeListener { _, isChecked ->
            ConfigUtils.setRestartNfcEnabled(this, isChecked)
            if (isChecked) Toast.makeText(this, getString(R.string.toast_force_refresh_on), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initRulesViews() {
        recyclerView = viewRules.findViewById(R.id.recyclerView)
        tvRuleCurrentName = viewRules.findViewById(R.id.tvRuleCurrentName)
        tvRuleCurrentPkg = viewRules.findViewById(R.id.tvRuleCurrentPkg)
        tvRuleCurrentComp = viewRules.findViewById(R.id.tvRuleCurrentComp)
        layoutRuleInfoMain = viewRules.findViewById(R.id.layoutRuleInfoMain)
        layoutRuleInfoDetails = viewRules.findViewById(R.id.layoutRuleInfoDetails)
        imgRuleInfoExpand = viewRules.findViewById(R.id.imgRuleInfoExpand)

        layoutRuleInfoMain.setOnClickListener {
            val isVisible = layoutRuleInfoDetails.visibility == View.VISIBLE
            layoutRuleInfoDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            imgRuleInfoExpand.animate().rotation(if (isVisible) 0f else 180f).setDuration(200).start()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NfcAppsAdapter(appList) { item, isChecked ->
            toggleRule(item, isChecked)
        }
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
        if (viewRules.visibility == View.VISIBLE) {
            refreshRulesList()
            refreshRulesInfo()
        }
        cbRestartNfc.isChecked = ConfigUtils.isRestartNfcEnabled(this)
    }

    private fun refreshRulesInfo() {
        try {
            val current = Settings.Secure.getString(contentResolver, "nfc_payment_default_component")
            if (current.isNullOrEmpty()) {
                tvRuleCurrentName.text = getString(R.string.status_none)
                tvRuleCurrentPkg.text = "-"
                tvRuleCurrentComp.text = "-"
            } else {
                val allServices = NfcServiceScanner.scan(this)
                val match = allServices.find { it.componentStr == current }
                tvRuleCurrentName.text = match?.label ?: getString(R.string.status_unknown)
                tvRuleCurrentPkg.text = current.split("/").getOrNull(0) ?: "-"
                tvRuleCurrentComp.text = current
            }
        } catch (e: Exception) {
            tvRuleCurrentName.text = getString(R.string.status_read_fail)
            tvRuleCurrentPkg.text = getString(R.string.need_adb_permission)
            tvRuleCurrentComp.text = "-"
        }
    }

    private fun refreshDashboard() {
        isAdbGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        updateStatusUI(cardAdb, tvAdbStatus, imgAdbIcon, isAdbGranted, getString(R.string.status_adb_granted), getString(R.string.status_adb_denied), R.drawable.icon_adb_service)

        isAccessEnabled = isAccessibilityServiceEnabled(this, NfcSwitcherService::class.java)
        updateStatusUI(cardAccess, tvAccessStatus, imgAccessIcon, isAccessEnabled, getString(R.string.status_access_running), getString(R.string.status_access_stopped), R.drawable.icon_accessibility)

        try {
            val current = Settings.Secure.getString(contentResolver, "nfc_payment_default_component")
            if (current.isNullOrEmpty()) {
                tvCurrentNfcStatus.text = getString(R.string.status_none)
            } else {
                val allServices = NfcServiceScanner.scan(this)
                val match = allServices.find { it.componentStr == current }
                tvCurrentNfcStatus.text = match?.label ?: getString(R.string.status_unknown)
            }
        } catch (e: Exception) {
            tvCurrentNfcStatus.text = getString(R.string.status_read_fail)
        }

        val screenOffRule = ConfigUtils.getScreenOffRule(this)
        tvScreenOffDesc.text = if (screenOffRule != null) getString(R.string.pref_screen_off_set, screenOffRule.nfcLabel) else getString(R.string.pref_screen_off_none)
    }

    private fun updateStatusUI(card: CardView, tv: TextView, icon: ImageView, isOk: Boolean, okText: String, failText: String, defaultIconRes: Int) {
        if (isOk) {
            tv.text = okText
            tv.setTextColor(ContextCompat.getColor(this, R.color.status_ok_text))
            icon.setImageResource(R.drawable.icon_check_circle)
            icon.setColorFilter(ContextCompat.getColor(this, R.color.status_ok_text))
            icon.backgroundTintList = null
        } else {
            tv.text = failText
            tv.setTextColor(ContextCompat.getColor(this, R.color.status_error_text))
            icon.setImageResource(defaultIconRes)
            icon.setColorFilter(ContextCompat.getColor(this, R.color.status_error_text))
        }
    }

    private fun refreshRulesList() {
        appList.clear()
        val scannedServices = NfcServiceScanner.scan(this)
        val savedRules = ConfigUtils.getRules(this)
        for (service in scannedServices) {
            val isEnabled = savedRules.any { it.packageName == service.triggerPackage }
            appList.add(AppUiModel(service, isEnabled))
        }
        adapter.notifyDataSetChanged()
    }

    private fun toggleRule(item: AppUiModel, isChecked: Boolean) {
        val rules = ConfigUtils.getRules(this)
        if (isChecked) {
            val rule = NfcRule(item.serviceInfo.label, item.serviceInfo.triggerPackage, item.serviceInfo.componentStr, item.serviceInfo.label)
            rules.removeAll { it.packageName == rule.packageName }
            rules.add(rule)
        } else {
            rules.removeAll { it.packageName == item.serviceInfo.triggerPackage }
        }
        ConfigUtils.saveRules(this, rules)
        item.isEnabled = isChecked
    }

    private fun showScreenOffDialog() {
        val scannedServices = NfcServiceScanner.scan(this)
        if (scannedServices.isEmpty()) return
        val labels = scannedServices.map { it.label }.toTypedArray()
        AlertDialog.Builder(this).setTitle(getString(R.string.dialog_screen_off_title)).setItems(labels) { _, which ->
            val selected = scannedServices[which]
            val rule = NfcRule(getString(R.string.rule_screen_off), "SCREEN_OFF", selected.componentStr, selected.label)
            ConfigUtils.saveScreenOffRule(this, rule)
            refreshDashboard()
        }.setPositiveButton(getString(R.string.btn_cancel), null).setNeutralButton(getString(R.string.btn_clear_settings)) { _, _ ->
            ConfigUtils.saveScreenOffRule(this, null)
            refreshDashboard()
        }.show()
    }

    private fun showAdbHelpDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_adb_help, null)
        val tvCommand = dialogView.findViewById<TextView>(R.id.tvCommand)
        val btnCopy = dialogView.findViewById<ImageButton>(R.id.btnCopy)

        val command = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
        tvCommand.text = command

        val copyAction = {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ADB Command", command)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.toast_cmd_copied), Toast.LENGTH_SHORT).show()
        }

        tvCommand.setOnClickListener { copyAction() }
        btnCopy.setOnClickListener { copyAction() }

        AlertDialog.Builder(this).setTitle(getString(R.string.dialog_adb_title)).setView(dialogView).setPositiveButton(getString(R.string.btn_understand), null).show()
    }

    private fun showAccessHelpDialog() {
        AlertDialog.Builder(this).setTitle(getString(R.string.dialog_access_title)).setMessage(getString(R.string.dialog_access_msg)).setPositiveButton(getString(R.string.btn_go_settings)) { _, _ ->
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = android.content.ComponentName(context, serviceClass)
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = android.content.ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    class NfcAppsAdapter(
        private val items: List<AppUiModel>,
        private val onToggle: (AppUiModel, Boolean) -> Unit
    ) : RecyclerView.Adapter<NfcAppsAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val itemContainer: View = v.findViewById(R.id.itemContainer)
            val tvName: TextView = v.findViewById(R.id.tvAppName)
            val tvIconText: TextView = v.findViewById(R.id.tvIconText)
            val switchEnable: SwitchMaterial = v.findViewById(R.id.switchEnable)
            val layoutDetails: View = v.findViewById(R.id.layoutDetails)
            val tvAppPkg: TextView = v.findViewById(R.id.tvAppPkg)
            val tvNfcComponent: TextView = v.findViewById(R.id.tvNfcComponent)
            val imgExpand: ImageView = v.findViewById(R.id.imgExpand)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_nfc_app, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.tvName.text = item.serviceInfo.label
            holder.tvIconText.text = item.serviceInfo.label.firstOrNull()?.toString() ?: "A"
            holder.tvAppPkg.text = item.serviceInfo.triggerPackage
            holder.tvNfcComponent.text = item.serviceInfo.componentStr

            holder.switchEnable.setOnCheckedChangeListener(null)
            holder.switchEnable.isChecked = item.isEnabled
            holder.switchEnable.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item, isChecked)
            }

            val isExpanded = item.isExpanded
            holder.layoutDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.imgExpand.rotation = if (isExpanded) 180f else 0f

            holder.itemContainer.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(position)
            }
        }

        override fun getItemCount() = items.size
    }
}