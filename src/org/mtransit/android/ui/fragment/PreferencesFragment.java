package org.mtransit.android.ui.fragment;

import org.mtransit.android.R;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.util.VendingUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class PreferencesFragment extends MTPreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener,
		VendingUtils.OnVendingResultListener {

	private static final String TAG = PreferenceFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String SUPPORT_SUBSCRIPTIONS_PREF = "pSupportSubs";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		findPreference(SUPPORT_SUBSCRIPTIONS_PREF).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Boolean hasSubscription = VendingUtils.isHasSubscription(PreferencesFragment.this.getActivity());
				if (hasSubscription == null) {
				} else if (hasSubscription) {
					StoreUtils.viewAppPage(getActivity(), getActivity().getPackageName());
				} else {
					VendingUtils.purchase(getActivity(), VendingUtils.MONTHLY_SUBSCRIPTION_SKU);
				}
				return false;
			}
		});
	}

	@Override
	public void onVendingResult(Boolean hasSubscription) {
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

	@Override
	public void onResume() {
		super.onResume();
		VendingUtils.onResume(getActivity(), this);
		PreferenceUtils.getPrefDefault(getActivity()).registerOnSharedPreferenceChangeListener(this);
		setUnitSummary(getActivity());
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
