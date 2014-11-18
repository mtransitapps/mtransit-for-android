package org.mtransit.android.ui.fragment;

import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.util.AnalyticsUtils;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

public abstract class ABFragment extends MTFragmentV4 implements AnalyticsUtils.Trackable, DataSourceProvider.ModulesUpdateListener {

	public static final int NO_ICON = -1;

	public static final Integer NO_BG_COLOR = null;

	public static final View NO_CUSTOM_VIEW = null;

	public static final boolean DEFAULT_THEME_DARK_INSTEAD_OF_LIGHT = false;

	public static final boolean DEFAULT_DISPLAY_HOME_AS_UP_ENABLED = true;

	public static final boolean DEFAULT_SHOW_SEARCH_MENU_ITEM = true;

	public boolean isABReady() {
		return true;
	}

	public CharSequence getABTitle(Context context) {
		return null;
	}

	public CharSequence getABSubtitle(Context context) {
		return null;
	}

	public int getABIconDrawableResId() {
		return ABFragment.NO_ICON;
	}

	public Integer getABBgColor() {
		return ABFragment.NO_BG_COLOR;
	}

	public View getABCustomView() {
		return ABFragment.NO_CUSTOM_VIEW;
	}

	public boolean isABDisplayHomeAsUpEnabled() {
		return DEFAULT_DISPLAY_HOME_AS_UP_ENABLED;
	}

	public boolean isABShowSearchMenuItem() {
		return DEFAULT_SHOW_SEARCH_MENU_ITEM;
	}

	public boolean isABThemeDarkInsteadOfThemeLight() {
		return DEFAULT_THEME_DARK_INSTEAD_OF_LIGHT;
	}

	public ActionBarController getAbController() {
		return ((MainActivity) getActivity()).getAbController();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DataSourceProvider.addModulesUpdateListerner(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		AnalyticsUtils.trackScreenView(getActivity(), this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DataSourceProvider.removeModulesUpdateListerner(this);
	}
}
