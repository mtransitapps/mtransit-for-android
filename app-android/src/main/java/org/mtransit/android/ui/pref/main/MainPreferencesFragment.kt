@file:JvmName("PreferencesFragment") // ANALYTICS
package org.mtransit.android.ui.pref.main

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.billing.BillingUtils
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.DeviceUtils
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.StoreUtils
import org.mtransit.android.commons.capitalize
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.MTDialog
import org.mtransit.android.ui.applyWindowInsetsEdgeToEdge
import org.mtransit.android.ui.feedback.FeedbackDialog
import org.mtransit.android.ui.modules.ModulesActivity
import org.mtransit.android.ui.pref.PreferencesViewModel
import org.mtransit.android.ui.setUpListEdgeToEdge
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.util.BatteryOptimizationIssueUtils
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.LanguageManager
import org.mtransit.android.util.LinkUtils
import org.mtransit.android.util.NightModeUtils
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class MainPreferencesFragment : PreferenceFragmentCompat(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = MainPreferencesFragment::class.java.simpleName

        private const val FORCE_OPEN_IN_EXTERNAL_BROWSER = false
    }

    override fun getLogTag(): String = LOG_TAG

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var billingManager: IBillingManager

    @Inject
    lateinit var demoModeManager: DemoModeManager

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var languageManager: LanguageManager

    private val viewModel by viewModels<MainPreferencesViewModel>()
    private val activityViewModel by activityViewModels<PreferencesViewModel>()

    private val onBillingResultListener = object : IBillingManager.OnBillingResultListener {
        override fun onBillingResult(productId: String?) {
            val hasSubscription: Boolean? = productId?.isNotEmpty()
            (findPreference(MainPreferencesViewModel.SUPPORT_SUBSCRIPTIONS_PREF) as? Preference)?.apply {
                when {
                    hasSubscription == null -> {
                        setTitle(commonsR.string.ellipsis)
                        setSummary(commonsR.string.ellipsis)
                        isEnabled = false
                    }

                    hasSubscription -> {
                        setTitle(R.string.support_subs_cancel_pref_title)
                        setSummary(R.string.support_subs_cancel_pref_summary)
                        isEnabled = true
                    }

                    else -> {
                        setTitle(R.string.support_subs_pref_title)
                        setSummary(R.string.support_subs_pref_summary)
                        isEnabled = true
                    }
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_DATE_AND_TIME_PREF) as? Preference)?.setOnPreferenceClickListener {
            DeviceUtils.showDateSettings(it.context)
            true // handled
        }
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_LOCATION_PREF) as? Preference)?.setOnPreferenceClickListener {
            DeviceUtils.showLocationSourceSettings(it.context)
            true // handled
        }
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_POWER_MANAGEMENT_PREF) as? Preference)?.setOnPreferenceClickListener {
            showPowerManagementDialog(
                activity ?: return@setOnPreferenceClickListener false, // not handled
            )
            true // handled
        }
        (findPreference(MainPreferencesViewModel.FEEDBACK_EMAIL_PREF) as? Preference)?.setOnPreferenceClickListener {
            FragmentUtils.replaceDialogFragment(
                activity ?: return@setOnPreferenceClickListener false, // not handled
                FragmentUtils.DIALOG_TAG,
                FeedbackDialog.newInstance(),
                null
            )
            true // handled
        }
        (findPreference(MainPreferencesViewModel.FEEDBACK_STORE_PREF) as? Preference)?.setOnPreferenceClickListener {
            StoreUtils.viewAppPage(it.context, Constants.MAIN_APP_PACKAGE_NAME, it.context.getString(commonsR.string.google_play))
            true // handled
        }
        (findPreference(MainPreferencesViewModel.SUPPORT_SUBSCRIPTIONS_PREF) as? Preference)?.setOnPreferenceClickListener {
            when (viewModel.hasSubscription.value) {
                null -> {} // DO NOTHING
                true -> activity?.let {
                    StoreUtils.viewSubscriptionPage(
                        it,
                        viewModel.currentSubscription.value.orEmpty(),
                        it.packageName,
                        it.getString(commonsR.string.google_play)
                    )
                } ?: run { return@setOnPreferenceClickListener false } // not handled
                false -> BillingUtils.showPurchaseDialog(
                    activity ?: return@setOnPreferenceClickListener false // not handled
                )
            }
            true // handled
        }
        (findPreference(MainPreferencesViewModel.SOCIAL_FACEBOOK_PREF) as? Preference)?.setOnPreferenceClickListener {
            val activity = activity ?: return@setOnPreferenceClickListener false // not handled
            LinkUtils.open(null, activity, MainPreferencesViewModel.FACEBOOK_PAGE_URL, activity.getString(R.string.facebook), FORCE_OPEN_IN_EXTERNAL_BROWSER)
            true // handled
        }
        (findPreference(MainPreferencesViewModel.SOCIAL_TWITTER_PREF) as? Preference)?.setOnPreferenceClickListener {
            val activity = activity ?: return@setOnPreferenceClickListener false // not handled
            LinkUtils.open(null, activity, MainPreferencesViewModel.TWITTER_PAGE_URL, activity.getString(R.string.twitter), FORCE_OPEN_IN_EXTERNAL_BROWSER)
            true // handled
        }
        (findPreference(MainPreferencesViewModel.ABOUT_PRIVACY_POLICY_PREF) as? Preference)?.setOnPreferenceClickListener {
            val activity = activity ?: return@setOnPreferenceClickListener false // not handled
            val url = if (LocaleUtils.isFR()) MainPreferencesViewModel.PRIVACY_POLICY_FR_PAGE_URL else MainPreferencesViewModel.PRIVACY_POLICY_PAGE_URL
            LinkUtils.open(null, activity, url, activity.getString(R.string.privacy_policy), FORCE_OPEN_IN_EXTERNAL_BROWSER)
            true // handled
        }
        (findPreference(MainPreferencesViewModel.ABOUT_TERMS_OF_USE_PREF) as? Preference)?.setOnPreferenceClickListener {
            val activity = activity ?: return@setOnPreferenceClickListener false // not handled
            val url = if (LocaleUtils.isFR()) MainPreferencesViewModel.TERMS_OF_USE_FR_PAGE_URL else MainPreferencesViewModel.TERMS_OF_USE_PAGE_URL
            LinkUtils.open(null, activity, url, activity.getString(R.string.terms_of_use), FORCE_OPEN_IN_EXTERNAL_BROWSER)
            true // handled
        }
        (findPreference(MainPreferencesViewModel.THIRD_PARTY_GOOGLE_PRIVACY_POLICY_PREF) as? Preference)?.setOnPreferenceClickListener {
            val activity = activity ?: return@setOnPreferenceClickListener false // not handled
            LinkUtils.open(null, activity, MainPreferencesViewModel.GOOGLE_PRIVACY_POLICY_PAGE_URL, null, FORCE_OPEN_IN_EXTERNAL_BROWSER)
            true // handled
        }
        (findPreference(MainPreferencesViewModel.THIRD_PARTY_YOUTUBE_TERMS_OF_SERVICE_PREF) as? Preference)?.setOnPreferenceClickListener {
            val activity = activity ?: return@setOnPreferenceClickListener false // not handled
            LinkUtils.open(null, activity, MainPreferencesViewModel.YOUTUBE_TERMS_OF_SERVICE_PAGE_URL, null, FORCE_OPEN_IN_EXTERNAL_BROWSER)
            true // handled
        }
        (findPreference(MainPreferencesViewModel.DEV_MODE_MODULE_PREF) as? Preference)?.setOnPreferenceClickListener {
            startActivity(ModulesActivity.newInstance(it.context))
            true // handled
        }
        (findPreference(MainPreferencesViewModel.DEV_MODE_CONSENT_RESET_PREF) as? Preference)?.setOnPreferenceClickListener {
            viewModel.resetConsent()
            true // handled
        }
        (findPreference(MainPreferencesViewModel.DEV_MODE_REWARDED_RESET_PREF) as? Preference)?.setOnPreferenceClickListener {
            viewModel.resetRewardedAd()
            true // handled
        }
        (findPreference(MainPreferencesViewModel.DEV_MODE_AD_INSPECTOR_PREF) as? Preference)?.setOnPreferenceClickListener {
            viewModel.openAdInspector()
            true // handled
        }
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_POWER_MANAGEMENT_PREF) as? Preference)?.apply {
            isEnabled = true
        }
        (findPreference(MainPreferencesViewModel.ABOUT_APP_VERSION_PREF) as? Preference)?.apply {
            context.let {
                summary = "" +
                        " v" + PackageManagerUtils.getAppVersionName(it) +
                        " r" + PackageManagerUtils.getAppVersionCode(it) +
                        " (" + BuildConfig.GIT_HASH + ")"
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showPowerManagementDialog(activity: Activity) {
        val view = activity.layoutInflater.inflate(R.layout.layout_battery_optimization_issue, null, false).apply {
            findViewById<TextView>(R.id.battery_optimization_issue_text_1).apply {
                text = getString(R.string.battery_optimization_issue_message_1_and_manufacturer, BatteryOptimizationIssueUtils.manufacturer.capitalize())
            }
            findViewById<ImageView>(R.id.battery_optimization_issue_img).apply {
                BatteryOptimizationIssueUtils.getDoNotKillMyAppImageUrlExtended()?.let { imageUrl ->
                    imageManager.loadInto(activity, imageUrl, this)
                    this.isVisible = true
                }
                setOnClickListener {
                    LinkUtils.open(
                        null,
                        activity,
                        BatteryOptimizationIssueUtils.getDoNotKillMyAppUrlExtended(),
                        BatteryOptimizationIssueUtils.DO_NOT_KILL_MY_APP_LABEL,
                        FORCE_OPEN_IN_EXTERNAL_BROWSER
                    )
                }
            }
            findViewById<TextView>(R.id.battery_optimization_issue_text_2).apply {
                text = LinkUtils.linkifyHtml(getString(R.string.battery_optimization_issue_message_2), false)
                movementMethod = LinkUtils.LinkMovementMethodInterceptor.getInstance { view, url ->
                    LinkUtils.open(view, activity, url, getString(commonsR.string.web_browser), true)
                }
            }
            findViewById<TextView>(R.id.battery_optimization_issue_custom).apply {
                isVisible = if (BatteryOptimizationIssueUtils.isSamsungDevice()) {
                    setText(R.string.battery_optimization_samsung_use_device_care)
                    true
                } else {
                    false
                }
            }
        }
        MTDialog.Builder(activity).apply {
            setTitle(R.string.battery_optimization_issue_title)
            setView(view)
            setPositiveButton(R.string.battery_optimization_issue_act) { dialog, _ ->
                dialog.dismiss()
                getActivity()?.let {
                    BatteryOptimizationIssueUtils.openDeviceBatteryOptimizationSettings(it)
                }
            }
            setNeutralButton(R.string.battery_optimization_issue_learn_more) { dialog, _ ->
                dialog.dismiss()
                getActivity()?.let {
                    LinkUtils.open(
                        null,
                        it,
                        BatteryOptimizationIssueUtils.getDoNotKillMyAppUrlExtended(),
                        BatteryOptimizationIssueUtils.DO_NOT_KILL_MY_APP_LABEL,
                        FORCE_OPEN_IN_EXTERNAL_BROWSER
                    )
                }
            }
            setCancelable(true)
        }.create().show()
    }

    override fun onResume() {
        super.onResume()
        billingManager.addListener(this.onBillingResultListener)
        viewModel.refreshData()
    }

    override fun onPause() {
        super.onPause()
        billingManager.removeListener(this.onBillingResultListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.setUpListEdgeToEdge()
        listView.applyWindowInsetsEdgeToEdge(WindowInsetsCompat.Type.navigationBars()) { insets ->
            updatePaddingRelative(
                bottom = insets.bottom
            )
        }
        viewModel.currentSubscription.observe(viewLifecycleOwner) {
            // do nothing
        }
        viewModel.hasSubscription.observe(viewLifecycleOwner) {
            // do nothing
        }
        activityViewModel.showSupport.observe(viewLifecycleOwner) { showSupport ->
            if (showSupport == true) {
                activityViewModel.onSupportShown() // clear flag before showing dialog
                if (viewModel.hasSubscription.value == false) {
                    BillingUtils.showPurchaseDialog(activity)
                }
            }
        }
        viewModel.units.observe(viewLifecycleOwner) { units ->
            (findPreference(DefaultPreferenceRepository.PREFS_UNITS) as? Preference)?.apply {
                setSummary(
                    when (units) {
                        DefaultPreferenceRepository.PREFS_UNITS_METRIC -> R.string.unit_pref_meter
                        DefaultPreferenceRepository.PREFS_UNITS_IMPERIAL -> R.string.unit_pref_imperial
                        else -> R.string.unit_pref_summary
                    }
                )
            }
        }
        if (FeatureFlags.F_ACCESSIBILITY_CONSUMER) {
            viewModel.showAccessibility.observe(viewLifecycleOwner) { showAccessibility ->
                (findPreference(DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY) as? Preference)?.apply {
                    setSummary(if (showAccessibility) R.string.show_accessibility_pref_summary_on else R.string.show_accessibility_pref_summary_off)
                }
            }
        } else {
            (findPreference(DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY) as? Preference)?.apply {
                preferenceScreen.removePreference(this)
            }
        }
        viewModel.useInternalWebBrowser.observe(viewLifecycleOwner) { useInternalWebBrowser ->
            (findPreference(DefaultPreferenceRepository.PREFS_USE_INTERNAL_WEB_BROWSER) as? Preference)?.apply {
                setSummary(
                    when (useInternalWebBrowser) {
                        true -> R.string.use_internal_web_browser_pref_summary_on
                        false -> R.string.use_internal_web_browser_pref_summary_off
                    }
                )
            }
        }
        viewModel.lang.observe(viewLifecycleOwner) { lang ->
            (findPreference(DefaultPreferenceRepository.PREFS_LANG) as? Preference)?.apply {
                setSummary(
                    when (lang) {
                        DefaultPreferenceRepository.PREFS_LANG_EN -> R.string.lang_pref_en
                        DefaultPreferenceRepository.PREFS_LANG_FR -> R.string.lang_pref_fr
                        DefaultPreferenceRepository.PREFS_LANG_SYSTEM_DEFAULT -> R.string.lang_pref_system_default
                        else -> R.string.lang_pref_system_default
                    }
                )
            }
            languageManager.updateAppLocaleFromUserPref()
        }
        viewModel.theme.observe(viewLifecycleOwner) { theme ->
            (findPreference(DefaultPreferenceRepository.PREFS_THEME) as? Preference)?.apply {
                setSummary(
                    when (theme) {
                        DefaultPreferenceRepository.PREFS_THEME_LIGHT -> R.string.theme_pref_light
                        DefaultPreferenceRepository.PREFS_THEME_DARK -> R.string.theme_pref_dark
                        DefaultPreferenceRepository.PREFS_THEME_SYSTEM_DEFAULT -> R.string.theme_pref_system_default
                        else -> R.string.theme_pref_system_default
                    }
                )
            }
            NightModeUtils.setDefaultNightMode(requireContext(), demoModeManager) // does NOT recreated because uiMode in configChanges AndroidManifest.xml
        }
        viewModel.devModeEnabled.observe(viewLifecycleOwner) { devModeEnabled ->
            val devModeGroupPref = findPreference(MainPreferencesViewModel.DEV_MODE_GROUP_PREF) as? PreferenceCategory ?: return@observe
            val devModeModulePref = findPreference(MainPreferencesViewModel.DEV_MODE_MODULE_PREF) as? Preference ?: return@observe
            val devModeResetConsentPref = findPreference(MainPreferencesViewModel.DEV_MODE_CONSENT_RESET_PREF) as? Preference ?: return@observe
            val devModeResetRewardedPref = findPreference(MainPreferencesViewModel.DEV_MODE_REWARDED_RESET_PREF) as? Preference ?: return@observe
            val devModeAdInspectorPref = findPreference(MainPreferencesViewModel.DEV_MODE_AD_INSPECTOR_PREF) as? Preference ?: return@observe
            if (devModeEnabled) {
                devModeGroupPref.isEnabled = true
                devModeModulePref.isEnabled = true
                devModeResetConsentPref.isEnabled = true
                devModeResetRewardedPref.isEnabled = true
                devModeAdInspectorPref.isEnabled = true
            } else {
                devModeGroupPref.isEnabled = false
                devModeModulePref.isEnabled = false
                devModeResetConsentPref.isEnabled = false
                devModeResetRewardedPref.isEnabled = false
                devModeAdInspectorPref.isEnabled = false
                devModeGroupPref.removePreference(devModeModulePref)
                devModeGroupPref.removePreference(devModeResetConsentPref)
                devModeGroupPref.removePreference(devModeResetRewardedPref)
                devModeGroupPref.removePreference(devModeAdInspectorPref)
                preferenceScreen.removePreference(devModeGroupPref)
            }
        }
    }
}
