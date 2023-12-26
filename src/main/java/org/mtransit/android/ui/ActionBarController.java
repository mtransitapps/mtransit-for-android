package org.mtransit.android.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.ABFragment;

import java.lang.ref.WeakReference;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ActionBarController implements Drawable.Callback, MTLog.Loggable {

	private static final String TAG = "Stack-" + ActionBarController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@Nullable
	private WeakReference<MainActivity> mainActivityWR;

	private boolean fragmentReady = false;

	private CharSequence fragmentTitle;

	private CharSequence fragmentSubtitle;

	@ColorInt
	private Integer fragmentBgColor = null;

	@Nullable
	private View fragmentCustomView = null;

	private boolean fragmentCustomViewFocusable = false;

	private boolean fragmentCustomViewRequestFocus = false;

	private boolean fragmentDisplayHomeAsUpEnabled = ABFragment.DEFAULT_DISPLAY_HOME_AS_UP_ENABLED;

	private boolean fragmentShowSearchMenuItem = ABFragment.DEFAULT_SHOW_SEARCH_MENU_ITEM;

	@Nullable
	private UpOnClickListener upOnClickListener;

	private ColorDrawable bgDrawable;

	public ActionBarController(@Nullable MainActivity mainActivity) {
		setMainActivity(mainActivity);
		init();
	}

	public void setMainActivity(@Nullable MainActivity mainActivity) {
		this.mainActivityWR = new WeakReference<>(mainActivity);
	}

	@Nullable
	private Context getContextOrNull() {
		ActionBar ab = getABOrNull();
		if (ab != null) {
			return ab.getThemedContext();
		}
		return getMainActivityOrNull();
	}

	@Nullable
	private MainActivity getMainActivityOrNull() {
		return this.mainActivityWR == null ? null : this.mainActivityWR.get();
	}

	@Nullable
	private ActionBar getABOrNull() {
		MainActivity mainActivity = getMainActivityOrNull();
		return mainActivity == null ? null : mainActivity.getSupportActionBar();
	}

	private final Handler handler = new Handler(Looper.getMainLooper());

	@Override
	public void invalidateDrawable(@Nullable Drawable who) {
		ActionBar ab = getABOrNull();
		if (ab != null) {
			ab.setBackgroundDrawable(who);
		}
	}

	@Override
	public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
		this.handler.postAtTime(what, when);
	}

	@Override
	public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
		this.handler.removeCallbacks(what);
	}

	private void init() {
		final MainActivity mainActivity = getMainActivityOrNull();
		if (mainActivity != null) {
			final Toolbar toolbar = mainActivity.findViewById(R.id.ab_toolbar);
			mainActivity.setSupportActionBar(toolbar);
			final ActionBar ab = getABOrNull();
			this.fragmentTitle = mainActivity.getTitle();
			this.fragmentSubtitle = ab == null ? null : ab.getSubtitle();
			if (ab != null) {
				ab.setElevation(0f);
				ab.hide();
				ab.setDisplayHomeAsUpEnabled(this.fragmentDisplayHomeAsUpEnabled);
				ab.setHomeButtonEnabled(true);
			}
			initBgDrawable(ab);
		}
	}

	public void setAB(@Nullable ABFragment abf) {
		Context context = getContextOrNull();
		if (abf != null && context != null) {
			setAB(
					abf.getABTitle(context),
					abf.getABSubtitle(context),
					abf.getABBgColor(context),
					abf.getABCustomView(),
					abf.isABCustomViewFocusable(),
					abf.isABCustomViewRequestFocus(),
					abf.isABThemeDarkInsteadOfThemeLight(),
					abf.isABDisplayHomeAsUpEnabled(),
					abf.isABShowSearchMenuItem(),
					abf.isABReady()
			);
		}
	}

	private void setAB(CharSequence title, CharSequence subtitle, @ColorInt Integer bgColor, View customView, boolean customViewFocusable,
					   boolean customViewRequestFocus, boolean themeDarkInsteadOfThemeLight, boolean displayHomeAsUpEnabled, boolean showSearchMenuItem,
					   boolean fragmentReady) {
		this.fragmentTitle = title;
		this.fragmentSubtitle = subtitle;
		this.fragmentBgColor = bgColor;
		this.fragmentCustomView = customView;
		this.fragmentCustomViewFocusable = customViewFocusable;
		this.fragmentCustomViewRequestFocus = customViewRequestFocus;
		this.fragmentDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		this.fragmentShowSearchMenuItem = showSearchMenuItem;
		this.fragmentReady = fragmentReady;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isCurrentFragmentVisible(@Nullable Fragment source) {
		final MainActivity mainActivity = getMainActivityOrNull();
		return mainActivity != null && mainActivity.isCurrentFragmentVisible(source);
	}

	public void setABReady(@Nullable Fragment source, boolean ready, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentReady = ready;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABTitle(@Nullable Fragment source, @Nullable CharSequence title, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentTitle = title;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABSubtitle(@Nullable Fragment source, @Nullable CharSequence subtitle, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentSubtitle = subtitle;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABBgColor(@Nullable Fragment source, @ColorInt @Nullable Integer bgColor, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentBgColor = bgColor;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABCustomView(@Nullable Fragment source, @Nullable View customView, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomView = customView;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABCustomViewFocusable(@Nullable Fragment source, boolean fragmentCustomViewFocusable, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomViewFocusable = fragmentCustomViewFocusable;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABCustomViewRequestFocus(@Nullable Fragment source, boolean fragmentCustomViewRequestFocus, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomViewRequestFocus = fragmentCustomViewRequestFocus;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABDisplayHomeAsUpEnabled(@Nullable Fragment source, boolean displayHomeAsUpEnabled, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABShowSearchMenuItem(@Nullable Fragment source, boolean showSearchMenuItem, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentShowSearchMenuItem = showSearchMenuItem;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void updateAB() {
		updateABDrawerClosed();
	}

	public void updateABDrawerClosed() {
		final ActionBar ab = getABOrNull();
		if (ab == null) {
			return;
		}
		if (!this.fragmentReady) {
			return;
		}
		if (this.fragmentCustomView != null) {
			if (!this.fragmentCustomView.equals(ab.getCustomView())) {
				ab.setCustomView(this.fragmentCustomView);
			}
			if (!this.fragmentDisplayHomeAsUpEnabled) {
				ab.getCustomView().setOnClickListener(getUpOnClickListener(getMainActivityOrNull()));
			}
			if (this.fragmentCustomViewFocusable) {
				this.fragmentCustomView.setFocusable(true);
				this.fragmentCustomView.setFocusableInTouchMode(true);
				if (this.fragmentCustomViewRequestFocus) {
					this.fragmentCustomView.requestFocus();
					this.fragmentCustomView.requestFocusFromTouch();
				}
			}
			ab.setDisplayShowCustomEnabled(true);
		} else {
			ab.setDisplayShowCustomEnabled(false);
		}
		ab.setDisplayHomeAsUpEnabled(this.fragmentDisplayHomeAsUpEnabled);
		if (TextUtils.isEmpty(this.fragmentTitle)) {
			ab.setDisplayShowTitleEnabled(false);
		} else {
			ab.setTitle(this.fragmentTitle);
			ab.setSubtitle(this.fragmentSubtitle);
			ab.setDisplayShowTitleEnabled(true);
		}
		if (this.fragmentBgColor != null) {
			setBgColor(ab, this.fragmentBgColor);
		}
		final MainActivity mainActivity = getMainActivityOrNull();
		if (mainActivity != null) {
			mainActivity.updateNavigationDrawerToggleIndicator();
		}
		updateMenuItemsVisibility(); // action bar icons are options menu items
		ab.show();
	}

	public void updateABBgColor() {
		if (!this.fragmentReady) {
			return;
		}
		if (this.fragmentBgColor != null) {
			setBgColor(getABOrNull(), this.fragmentBgColor);
		}
	}

	private void setBgColor(ActionBar ab, @ColorInt int colorInt) {
		final ColorDrawable bgDrawable = getBgDrawableOrNull(ab);
		if (bgDrawable != null) {
			bgDrawable.setColor(colorInt);
		}
	}

	@Nullable
	private ColorDrawable getBgDrawableOrNull(ActionBar ab) {
		if (this.bgDrawable == null) {
			initBgDrawable(ab);
		}
		return this.bgDrawable;
	}

	private void initBgDrawable(ActionBar ab) {
		if (ab != null) {
			this.bgDrawable = new ColorDrawable();
			ab.setBackgroundDrawable(this.bgDrawable);
		}
	}

	@Nullable
	private UpOnClickListener getUpOnClickListener(@Nullable MainActivity mainActivity) {
		if (this.upOnClickListener == null) {
			if (mainActivity == null) {
				mainActivity = getMainActivityOrNull();
			}
			if (mainActivity != null) {
				this.upOnClickListener = new UpOnClickListener(mainActivity);
			}
		}
		return this.upOnClickListener;
	}

	public void onSaveState(@NonNull Bundle outState) {
		// DO NOTHING
	}

	public void onRestoreState(@NonNull Bundle savedInstanceState) {
		// DO NOTHING
	}

	public void destroy() {
		if (this.mainActivityWR != null) {
			this.mainActivityWR.clear();
			this.mainActivityWR = null;
		}
		this.fragmentCustomView = null;
		this.upOnClickListener = null;
	}

	@Nullable
	private Boolean hasAgenciesEnabled = null;

	public void onHasAgenciesEnabledUpdated(@Nullable Boolean hasAgenciesEnabled) {
		this.hasAgenciesEnabled = hasAgenciesEnabled;
		updateMenuItemsVisibility();
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.menu_main, menu);
		this.searchMenuItem = menu.findItem(R.id.nav_search);
		updateMenuItemsVisibility();
		return true;
	}

	@Nullable
	private MenuItem searchMenuItem = null;

	public void updateMenuItemsVisibility() {
		if (this.searchMenuItem != null) {
			this.searchMenuItem.setVisible(
					Boolean.TRUE.equals(this.hasAgenciesEnabled)
							&& this.fragmentShowSearchMenuItem
			);
		}
	}

	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		MainActivity mainActivity = getMainActivityOrNull();
		if (mainActivity != null) {
			if (item.getItemId() == android.R.id.home) {
				if (mainActivity.onUpIconClick()) {
					return true; // handled
				}
			}
			if (item.getItemId() == R.id.nav_search) {
				mainActivity.onSearchRequested();
				return true; // handled
			}
		}
		return false; // not handled
	}

	private static class UpOnClickListener implements View.OnClickListener {

		@NonNull
		private final WeakReference<MainActivity> mainActivityWR;

		UpOnClickListener(MainActivity mainActivity) {
			this.mainActivityWR = new WeakReference<>(mainActivity);
		}

		@Override
		public void onClick(@NonNull View view) {
			final MainActivity mainActivity = this.mainActivityWR.get();
			if (mainActivity != null) {
				mainActivity.onUpIconClick();
			}
		}
	}

	public interface ActionBarColorizer {
		@Nullable
		Integer getBgColor(int position);
	}

	public static class SimpleActionBarColorizer implements ActionBarColorizer {

		@Nullable
		private int[] bgColors;

		@Nullable
		@Override
		public final Integer getBgColor(int position) {
			return bgColors == null ? null : bgColors[position % bgColors.length];
		}

		public void setBgColors(@NonNull int... colors) {
			bgColors = colors;
		}
	}
}
