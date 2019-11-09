package org.mtransit.android.ui.fragment;

import org.mtransit.android.R;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.analytics.AnalyticsManager;
import org.mtransit.android.ui.view.common.IActivity;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.view.View;

public abstract class ABFragment extends MTFragment implements AnalyticsManager.Trackable, IActivity, DataSourceProvider.ModulesUpdateListener {

	private static final boolean DEFAULT_THEME_DARK_INSTEAD_OF_LIGHT = false;

	public static final boolean DEFAULT_DISPLAY_HOME_AS_UP_ENABLED = true;

	public static final boolean DEFAULT_SHOW_SEARCH_MENU_ITEM = true;

	@NonNull
	private final IAnalyticsManager analyticsManager;

	public ABFragment() {
		super();
		analyticsManager = Injection.providesAnalyticsManager();
	}

	public boolean isABReady() {
		return true;
	}

	@Nullable
	public CharSequence getABTitle(Context context) {
		return null;
	}

	@Nullable
	public CharSequence getABSubtitle(Context context) {
		return null;
	}

	@ColorInt
	@Nullable
	private Integer defaultABBgColor = null;

	@ColorInt
	public Integer getABBgColor(Context context) {
		if (this.defaultABBgColor == null && context != null) {
			this.defaultABBgColor = ThemeUtils.resolveColorAttribute(context, R.attr.colorPrimary);
		}
		return this.defaultABBgColor;
	}

	@Nullable
	public View getABCustomView() {
		return null;
	}

	public boolean isABCustomViewFocusable() {
		return false;
	}

	public boolean isABCustomViewRequestFocus() {
		return false;
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

	@Nullable
	public ActionBarController getAbController() {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return null;
		}
		return ((MainActivity) activity).getAbController();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DataSourceProvider.addModulesUpdateListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		analyticsManager.trackScreenView(this, this);
		ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setAB(this);
			abController.updateAB();
		}
	}

	public boolean onBackPressed() {
		return false; // not processed
	}

	@Override
	public void onPause() {
		super.onPause();
		StatusLoader.get().clearAllTasks();
		ServiceUpdateLoader.get().clearAllTasks();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DataSourceProvider.removeModulesUpdateListener(this);
	}

	@Override
	public void finish() {
		requireActivity().finish();
	}

	@Nullable
	@Override
	public <T extends View> T findViewById(int id) {
		if (getView() == null) {
			return null;
		}
		return getView().findViewById(id);
	}
}
