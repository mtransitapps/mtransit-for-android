@file:JvmName("PreferencesFragment") // ANALYTICS
package org.mtransit.android.ui.pref.main

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
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
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.MTDialog
import org.mtransit.android.ui.modules.ModulesActivity
import org.mtransit.android.ui.pref.PreferencesViewModel
import org.mtransit.android.util.LinkUtils
import org.mtransit.android.util.NightModeUtils
import javax.inject.Inject

@AndroidEntryPoint
class MainPreferencesFragment : PreferenceFragmentCompat(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = MainPreferencesFragment::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var billingManager: IBillingManager

    @Inject
    lateinit var demoModeManager: DemoModeManager

    private val viewModel by viewModels<MainPreferencesViewModel>()
    private val activityViewModel by activityViewModels<PreferencesViewModel>()

    private val onBillingResultListener = object : IBillingManager.OnBillingResultListener {
        override fun onBillingResult(productId: String?) {
            val hasSubscription: Boolean? = productId?.isNotEmpty()
            (findPreference(MainPreferencesViewModel.SUPPORT_SUBSCRIPTIONS_PREF) as? Preference)?.apply {
                when {
                    hasSubscription == null -> {
                        setTitle(R.string.ellipsis)
                        setSummary(R.string.ellipsis)
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
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_LANGUAGE_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                DeviceUtils.showLocaleSettings(it)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_DATE_AND_TIME_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                DeviceUtils.showDateSettings(it)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_LOCATION_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                DeviceUtils.showLocationSourceSettings(it)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_POWER_MANAGEMENT_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                showPowerManagementDialog(it)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.FEEDBACK_EMAIL_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                LinkUtils.sendEmail(it, dataSourcesRepository)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.FEEDBACK_STORE_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                StoreUtils.viewAppPage(it, Constants.MAIN_APP_PACKAGE_NAME, it.getString(R.string.google_play))
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.SUPPORT_SUBSCRIPTIONS_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                val currentSubscription = viewModel.currentSubscription
                val hasSubscription: Boolean? = viewModel.hasSubscription
                when {
                    hasSubscription == null -> { // unknown status
                        // DO NOTHING
                    }
                    hasSubscription -> { // has subscription
                        StoreUtils.viewSubscriptionPage(it, currentSubscription!!, it.packageName, it.getString(R.string.google_play))
                    }
                    else -> { // does NOT have subscription
                        BillingUtils.showPurchaseDialog(it)
                    }
                }
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.SOCIAL_FACEBOOK_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                LinkUtils.open(null, it, MainPreferencesViewModel.FACEBOOK_PAGE_URL, it.getString(R.string.facebook), false)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.SOCIAL_TWITTER_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                LinkUtils.open(null, it, MainPreferencesViewModel.TWITTER_PAGE_URL, it.getString(R.string.twitter), false)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.ABOUT_PRIVACY_POLICY_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                LinkUtils.open(
                    null,
                    it,
                    if (LocaleUtils.isFR()) MainPreferencesViewModel.PRIVACY_POLICY_FR_PAGE_URL else MainPreferencesViewModel.PRIVACY_POLICY_PAGE_URL,
                    it.getString(R.string.privacy_policy),
                    false // open in external web browser
                )
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.ABOUT_TERMS_OF_USE_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                LinkUtils.open(
                    null,
                    it,
                    if (LocaleUtils.isFR()) MainPreferencesViewModel.TERMS_OF_USE_FR_PAGE_URL else MainPreferencesViewModel.TERMS_OF_USE_PAGE_URL,
                    it.getString(R.string.terms_of_use),
                    false // open in external web browser
                )
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.THIRD_PARTY_GOOGLE_PRIVACY_POLICY_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                LinkUtils.open(null, it, MainPreferencesViewModel.GOOGLE_PRIVACY_POLICY_PAGE_URL, null, false)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.THIRD_PARTY_YOUTUBE_TERMS_OF_SERVICE_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                LinkUtils.open(null, it, MainPreferencesViewModel.YOUTUBE_TERMS_OF_SERVICE_PAGE_URL, null, false)
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.DEV_MODE_MODULE_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                startActivity(ModulesActivity.newInstance(it))
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.DEV_MODE_REWARDED_RESET_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                viewModel.resetRewardedAd()
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.DEV_MODE_AD_INSPECTOR_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                viewModel.openAdInspector()
                true
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.DEV_MODE_AD_MEDIATION_TEST_PREF) as? Preference)?.setOnPreferenceClickListener {
            activity?.let {
                @Suppress("RedundantIf", "ConstantConditionIf")
                if (true) { // DANGEROUS !!!! ONLY FOR MANUAL TESTING!
                    false // not handled
                } else {
                    // Add tools:replace="android:supportsRtl" in AndroidManifest.xml
                    // com.google.android.ads.mediationtestsuite.MediationTestSuite.launch(activity);  // adds WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE...
                    true // handled
                }
            } ?: false
        }
        (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_POWER_MANAGEMENT_PREF) as? Preference)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isEnabled = true
            } else {
                (findPreference(MainPreferencesViewModel.DEVICE_SETTINGS_GROUP_PREF) as? PreferenceCategory)?.removePreference(this)
            }
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

    private fun showPowerManagementDialog(activity: Activity) {
        MTDialog.Builder(activity).apply {
            setTitle(R.string.battery_optimization_issue_title)
            setMessage(R.string.battery_optimization_issue_message)
            setPositiveButton(R.string.battery_optimization_issue_act) { dialog, _ ->
                dialog.dismiss()
                getActivity()?.let {
                    DeviceUtils.showIgnoreBatteryOptimizationSettings(it)
                }
            }
            setNeutralButton(R.string.battery_optimization_issue_learn_more) { dialog, _ ->
                dialog.dismiss()
                getActivity()?.let {
                    LinkUtils.open(
                        null,
                        it,
                        MainPreferencesViewModel.DO_NOT_KILL_MY_APP_URL,
                        MainPreferencesViewModel.DO_NOT_KILL_MY_APP_URL,
                        false
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
        activityViewModel.showSupport.observe(viewLifecycleOwner) { showSupport ->
            if (showSupport == true) {
                activityViewModel.onSupportShown() // clear flag before showing dialog
                if (viewModel.hasSubscription == false) {
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
            val devModeResetRewardedPref = findPreference(MainPreferencesViewModel.DEV_MODE_REWARDED_RESET_PREF) as? Preference ?: return@observe
            val devModeAdInspectorPref = findPreference(MainPreferencesViewModel.DEV_MODE_AD_INSPECTOR_PREF) as? Preference ?: return@observe
            val devModeAdMediationTestPref = findPreference(MainPreferencesViewModel.DEV_MODE_AD_MEDIATION_TEST_PREF) as? Preference ?: return@observe
            if (devModeEnabled) {
                devModeGroupPref.isEnabled = true
                devModeModulePref.isEnabled = true
                devModeResetRewardedPref.isEnabled = true
                devModeAdInspectorPref.isEnabled = true
                devModeAdMediationTestPref.isEnabled = true
            } else {
                devModeGroupPref.isEnabled = false
                devModeModulePref.isEnabled = false
                devModeResetRewardedPref.isEnabled = false
                devModeAdInspectorPref.isEnabled = false
                devModeAdMediationTestPref.isEnabled = false
                devModeGroupPref.removePreference(devModeModulePref)
                devModeGroupPref.removePreference(devModeResetRewardedPref)
                devModeGroupPref.removePreference(devModeAdInspectorPref)
                devModeGroupPref.removePreference(devModeAdMediationTestPref)
                preferenceScreen.removePreference(devModeGroupPref)
            }
        }
    }
}