package org.mtransit.android.ui.fragment;

import android.content.Context;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import org.mtransit.android.R;
import org.mtransit.android.analytics.AnalyticsManager;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.commons.FeatureFlags;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public abstract class ABFragment extends MTFragmentX implements AnalyticsManager.Trackable, IActivity {

	private static final boolean DEFAULT_THEME_DARK_INSTEAD_OF_LIGHT = false;

	public static final boolean DEFAULT_DISPLAY_HOME_AS_UP_ENABLED = true;

	public static final boolean DEFAULT_SHOW_SEARCH_MENU_ITEM = true;

	@Inject
	IAnalyticsManager analyticsManager;
	@Inject
	StatusLoader statusLoader;
	@Inject
	ServiceUpdateLoader serviceUpdateLoader;

	public ABFragment() {
		super();
	}

	@ContentView
	public ABFragment(@LayoutRes int contentLayoutId) {
		super(contentLayoutId);
	}

	public boolean isABReady() {
		return true; // default = true = ready
	}

	@Nullable
	public CharSequence getABTitle(@Nullable Context context) {
		return null;
	}

	@Nullable
	public CharSequence getABSubtitle(@Nullable Context context) {
		return null;
	}

	@ColorInt
	@Nullable
	private Integer defaultABBgColor = null;

	@Nullable
	@ColorInt
	public Integer getABBgColor(@Nullable Context context) {
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
		if (FeatureFlags.F_NAVIGATION) {
			return null;
		}
		final FragmentActivity activity = getActivity();
		if (!(activity instanceof MainActivity)) {
			return null;
		}
		return ((MainActivity) activity).getAbController();
	}

	@Override
	public void onResume() {
		super.onResume();
		analyticsManager.trackScreenView(this, this);
		final ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setAB(this);
			abController.updateAB();
		}
	}

	public boolean onBackPressed() {
		return false; // not processed
	}

	@CallSuper
	@Override
	public void onPause() {
		super.onPause();
		this.statusLoader.clearAllTasks();
		this.serviceUpdateLoader.clearAllTasks();
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

	@NonNull
	@Override
	public LifecycleOwner getLifecycleOwner() {
		return this;
	}
}
