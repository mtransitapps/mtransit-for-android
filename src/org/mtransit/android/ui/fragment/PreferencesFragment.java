package org.mtransit.android.ui.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.DeviceUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.ui.PreferencesActivity;
import org.mtransit.android.util.LinkUtils;
import org.mtransit.android.util.VendingUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

public class PreferencesFragment extends MTPreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener,
		VendingUtils.OnVendingResultListener {

	private static final String LOG_TAG = PreferenceFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String DEVICE_SETTINGS_GROUP_PREF = "pDeviceSettings";
	private static final String DEVICE_SETTINGS_LANGUAGE_PREF = "pDeviceSettingsLanguage";
	private static final String DEVICE_SETTINGS_DATE_AND_TIME_PREF = "pDeviceSettingsDateAndTime";
	private static final String DEVICE_SETTINGS_LOCATION_PREF = "pDeviceSettingsLocation";
	private static final String DEVICE_SETTINGS_POWER_MANAGEMENT_PREF = "pDeviceSettingsPowerManagement";

	private static final String FEEDBACK_EMAIL_PREF = "pFeedbackEmail";
	private static final String FEEDBACK_STORE_PREF = "pFeedbackStore";

	private static final String SUPPORT_SUBSCRIPTIONS_PREF = "pSupportSubs";

	private static final String ABOUT_PRIVACY_POLICY_PREF = "pAboutPrivacyPolicy";
	private static final String ABOUT_APP_VERSION_PREF = "pAboutAppVersion";

	private static final String SOCIAL_FACEBOOK_PREF = "pSocialFacebook";
	private static final String SOCIAL_TWITTER_PREF = "pSocialTwitter";

	private static final String TWITTER_PAGE_URL = "https://twitter.com/montransit";
	private static final String FACEBOOK_PAGE_URL = "https://facebook.com/MonTransit";
	private static final String DONT_KILL_MY_APP_URL = "https://dontkillmyapp.com/";
	private static final String PRIVACY_POLICY_PAGE_URL = "https://github.com/mtransitapps/mtransit-for-android/wiki/PrivacyPolicy";
	private static final String PRIVACY_POLICY_FR_PAGE_URL = "https://github.com/mtransitapps/mtransit-for-android/wiki/PrivacyPolicyFr";

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		findPreference(DEVICE_SETTINGS_LANGUAGE_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				DeviceUtils.showLocaleSettings(activity);
				return true; // handled
			}
		});
		findPreference(DEVICE_SETTINGS_DATE_AND_TIME_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				DeviceUtils.showDateSettings(activity);
				return true; // handled
			}
		});
		findPreference(DEVICE_SETTINGS_LOCATION_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				DeviceUtils.showLocationSourceSettings(activity);
				return true; // handled
			}
		});
		findPreference(DEVICE_SETTINGS_POWER_MANAGEMENT_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				new AlertDialog.Builder(activity)
						.setTitle(R.string.battery_optimization_issue_title)
						.setMessage(R.string.battery_optimization_issue_message)
						.setPositiveButton(R.string.battery_optimization_issue_act, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Activity activity = getActivity();
								if (activity == null) {
									return;
								}
								DeviceUtils.showIgnoreBatteryOptimizationSettings(activity);
							}
						})
						.setNeutralButton(R.string.battery_optimization_issue_learn_more, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Activity activity = getActivity();
								if (activity == null) {
									return;
								}
								LinkUtils.open(activity, DONT_KILL_MY_APP_URL, DONT_KILL_MY_APP_URL, false);
							}
						})
						.setCancelable(true)
						.create()
						.show();
				return true; // handled
			}
		});
		findPreference(FEEDBACK_EMAIL_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				LinkUtils.sendEmail(activity);
				return true; // handled
			}
		});
		findPreference(FEEDBACK_STORE_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				StoreUtils.viewAppPage(activity, Constants.MAIN_APP_PACKAGE_NAME, activity.getString(R.string.google_play));
				return true; // handled
			}
		});
		findPreference(SUPPORT_SUBSCRIPTIONS_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				Boolean hasSubscription = VendingUtils.isHasSubscription(activity);
				if (hasSubscription == null) {
					// DO NOTHING
				} else if (hasSubscription) {
					StoreUtils.viewAppPage(activity, activity.getPackageName(), activity.getString(R.string.google_play));
				} else {
					VendingUtils.purchase(activity);
				}
				return true; // handled
			}
		});
		findPreference(SOCIAL_FACEBOOK_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				LinkUtils.open(activity, FACEBOOK_PAGE_URL, activity.getString(R.string.facebook), false);
				return true; // handled
			}
		});
		findPreference(SOCIAL_TWITTER_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				LinkUtils.open(activity, TWITTER_PAGE_URL, activity.getString(R.string.twitter), false);
				return true; // handled
			}
		});
		findPreference(ABOUT_PRIVACY_POLICY_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Activity activity = getActivity();
				if (activity == null) {
					return false; // not handled
				}
				LinkUtils.open(activity,
						LocaleUtils.isFR() ?
								PRIVACY_POLICY_FR_PAGE_URL :
								PRIVACY_POLICY_PAGE_URL,
						activity.getString(R.string.privacy_policy), false);
				return true; // handled
			}
		});
	}

	@Override
	public void onVendingResult(@Nullable Boolean hasSubscription) {
		Preference supportSubsPref = findPreference(SUPPORT_SUBSCRIPTIONS_PREF);
		if (hasSubscription == null) {
			supportSubsPref.setTitle(R.string.ellipsis);
			supportSubsPref.setSummary(R.string.ellipsis);
			supportSubsPref.setEnabled(false);
		} else if (hasSubscription) {
			supportSubsPref.setTitle(R.string.support_subs_cancel_pref_title);
			supportSubsPref.setSummary(R.string.support_subs_cancel_pref_summary);
			supportSubsPref.setEnabled(true);
		} else {
			supportSubsPref.setTitle(R.string.support_subs_pref_title);
			supportSubsPref.setSummary(R.string.support_subs_pref_summary);
			supportSubsPref.setEnabled(true);
		}
	}

	@Override
	public void onSharedPreferenceChanged(@Nullable SharedPreferences sharedPreferences, @Nullable String key) {
		if (PreferenceUtils.PREFS_UNITS.equals(key)) {
			setUnitSummary(getActivity());
		} else if (PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER.equals(key)) {
			setUseInternalWebBrowserSummary(getActivity());
		}
	}

	private void setUnitSummary(Context context) {
		if (context == null) {
			return;
		}
		Preference unitPref = findPreference(PreferenceUtils.PREFS_UNITS);
		if (unitPref == null) {
			return;
		}
		String units = PreferenceUtils.getPrefDefault(context, PreferenceUtils.PREFS_UNITS, null);
		if (PreferenceUtils.PREFS_UNITS_METRIC.equals(units)) {
			unitPref.setSummary(R.string.unit_pref_meter);
		} else if (PreferenceUtils.PREFS_UNITS_IMPERIAL.equals(units)) {
			unitPref.setSummary(R.string.unit_pref_imperial);
		} else {
			unitPref.setSummary(R.string.unit_pref_summary);
		}
	}

	private void setUseInternalWebBrowserSummary(Context context) {
		if (context == null) {
			return;
		}
		Preference useInternalWebBrowserPref = findPreference(PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER);
		if (useInternalWebBrowserPref == null) {
			return;
		}
		boolean useInternalWebBrowser = PreferenceUtils.getPrefDefault(context, //
				PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER, PreferenceUtils.PREFS_USE_INTERNAL_WEB_BROWSER_DEFAULT);
		if (useInternalWebBrowser) {
			useInternalWebBrowserPref.setSummary(R.string.use_internal_web_browser_pref_summary_on);
		} else {
			useInternalWebBrowserPref.setSummary(R.string.use_internal_web_browser_pref_summary_off);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		VendingUtils.onResume(getActivity(), this);
		PreferenceUtils.getPrefDefault(getActivity()).registerOnSharedPreferenceChangeListener(this);
		setUnitSummary(getActivity());
		setUseInternalWebBrowserSummary(getActivity());
		setDeviceSettings();
		setAppVersion(getActivity());
		if (((PreferencesActivity) getActivity()).isShowSupport()) {
			((PreferencesActivity) getActivity()).setShowSupport(false); // clear flag before showing dialog
			Boolean hasSubscription = VendingUtils.isHasSubscription(getActivity());
			if (hasSubscription != null && !hasSubscription) {
				VendingUtils.purchase(getActivity());
			}
		}
	}

	private void setDeviceSettings() {
		Preference powerManagementPref = findPreference(DEVICE_SETTINGS_POWER_MANAGEMENT_PREF);
		if (powerManagementPref == null) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			powerManagementPref.setEnabled(true);
		} else {
			Preference deviceSettingsPref = findPreference(DEVICE_SETTINGS_GROUP_PREF);
			if (deviceSettingsPref != null) {
				((PreferenceCategory) deviceSettingsPref).removePreference(powerManagementPref);
			}
		}
	}

	private void setAppVersion(@NonNull Context context) {
		findPreference(ABOUT_APP_VERSION_PREF).setSummary("" //
				+ " v" + PackageManagerUtils.getAppVersionName(context) //
				+ " (" + PackageManagerUtils.getAppVersionCode(context) + ")");
	}

	@Override
	public void onPause() {
		super.onPause();
		PreferenceUtils.getPrefDefault(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
		VendingUtils.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		VendingUtils.destroyBilling(this);
	}
}
