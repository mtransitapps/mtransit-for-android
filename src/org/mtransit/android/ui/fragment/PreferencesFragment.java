package org.mtransit.android.ui.fragment;

import org.mtransit.android.R;
import org.mtransit.android.billing.IBillingManager;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.di.Injection;
import org.mtransit.android.ui.PreferencesActivity;
import org.mtransit.android.util.FragmentUtils;
import org.mtransit.android.util.LinkUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PreferencesFragment extends MTPreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener,
		IBillingManager.OnBillingResultListener {

	private static final String LOG_TAG = PreferencesFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String FEEDBACK_EMAIL_PREF = "pFeedbackEmail";
	private static final String FEEDBACK_STORE_PREF = "pFeedbackStore";

	private static final String SUPPORT_SUBSCRIPTIONS_PREF = "pSupportSubs";

	private static final String ABOUT_APP_VERSION_PREF = "pAboutAppVersion";

	private static final String SOCIAL_FACEBOOK_PREF = "pSocialFacebook";
	private static final String SOCIAL_TWITTER_PREF = "pSocialTwitter";

	private static final String TWITTER_PAGE_URL = "https://twitter.com/montransit";
	private static final String FACEBOOK_PAGE_URL = "https://facebook.com/MonTransit";

	@NonNull
	private final IBillingManager billingManager;

	public PreferencesFragment() {
		super();
		this.billingManager = Injection.providesBillingManager();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
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
				Boolean hasSubscription = billingManager.hasSubscription();
				if (hasSubscription == null) {
					// DO NOTHING
				} else if (hasSubscription) {
					StoreUtils.viewAppPage(activity, activity.getPackageName(), activity.getString(R.string.google_play));
				} else {
					FragmentUtils.replaceDialogFragment(activity, FragmentUtils.DIALOG_TAG, PurchaseDialogFragment.newInstance(), null);
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
	}

	@Override
	public void onBillingResult(@Nullable Boolean hasSubscription) {
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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
		this.billingManager.addListener(this);
		this.billingManager.refreshPurchases();
		PreferenceUtils.getPrefDefault(getActivity()).registerOnSharedPreferenceChangeListener(this);
		setUnitSummary(getActivity());
		setUseInternalWebBrowserSummary(getActivity());
		setAppVersion(getActivity());
		if (((PreferencesActivity) getActivity()).isShowSupport()) {
			((PreferencesActivity) getActivity()).setShowSupport(false); // clear flag before showing dialog
			Boolean hasSubscription = this.billingManager.hasSubscription();
			if (hasSubscription != null && !hasSubscription) {
				FragmentUtils.replaceDialogFragment(getActivity(), FragmentUtils.DIALOG_TAG, PurchaseDialogFragment.newInstance(), null);
			}
		}
	}

	private void setAppVersion(Context context) {
		findPreference(ABOUT_APP_VERSION_PREF).setSummary("" //
				+ " v" + PackageManagerUtils.getAppVersionName(context) //
				+ " (" + PackageManagerUtils.getAppVersionCode(context) + ")");
	}

	@Override
	public void onPause() {
		super.onPause();
		PreferenceUtils.getPrefDefault(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
		this.billingManager.removeListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.billingManager.destroy();
	}
}
