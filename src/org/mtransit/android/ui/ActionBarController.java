package org.mtransit.android.ui;

import java.lang.ref.WeakReference;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.view.MTOnClickListener;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class ActionBarController implements Drawable.Callback, MTLog.Loggable {

	private static final String TAG = "Stack-" + ActionBarController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private WeakReference<MainActivity> mainActivityWR;

	private boolean fragmentReady = false;

	private CharSequence fragmentTitle;

	private CharSequence fragmentSubtitle;

	@ColorInt
	private Integer fragmentBgColor = null;

	private View fragmentCustomView = null;

	private boolean fragmentCustomViewFocusable = false;

	private boolean fragmentCustomViewRequestFocus = false;

	private boolean fragmentDisplayHomeAsUpEnabled = ABFragment.DEFAULT_DISPLAY_HOME_AS_UP_ENABLED;

	private boolean fragmentShowSearchMenuItem = ABFragment.DEFAULT_SHOW_SEARCH_MENU_ITEM;

	private UpOnClickListener upOnClickListener;

	private ColorDrawable bgDrawable;

	public ActionBarController(MainActivity mainActivity) {
		setMainActivity(mainActivity);
		init();
	}

	public void setMainActivity(MainActivity mainActivity) {
		this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
	}

	private Context getContextOrNull() {
		ActionBar ab = getABOrNull();
		if (ab != null) {
			return ab.getThemedContext();
		}
		return getMainActivityOrNull();
	}

	private MainActivity getMainActivityOrNull() {
		return this.mainActivityWR == null ? null : this.mainActivityWR.get();
	}

	private ActionBar getABOrNull() {
		MainActivity mainActivity = getMainActivityOrNull();
		return mainActivity == null ? null : mainActivity.getSupportActionBar();
	}

	private final Handler handler = new Handler();

	@Override
	public void invalidateDrawable(Drawable who) {
		ActionBar ab = getABOrNull();
		if (ab != null) {
			ab.setBackgroundDrawable(who);
		}
	}

	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
		this.handler.postAtTime(what, when);
	}

	@Override
	public void unscheduleDrawable(Drawable who, Runnable what) {
		this.handler.removeCallbacks(what);
	}

	private void init() {
		MainActivity mainActivity = getMainActivityOrNull();
		if (mainActivity != null) {
			Toolbar toolbar = (Toolbar) mainActivity.findViewById(R.id.ab_toolbar);
			mainActivity.setSupportActionBar(toolbar);
			ActionBar ab = getABOrNull();
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
			setAB(abf.getABTitle(context), abf.getABSubtitle(context), abf.getABBgColor(context), abf.getABCustomView(), abf.isABCustomViewFocusable(),
					abf.isABCustomViewRequestFocus(), abf.isABThemeDarkInsteadOfThemeLight(), abf.isABDisplayHomeAsUpEnabled(), abf.isABShowSearchMenuItem(),
					abf.isABReady());
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

	private boolean isCurrentFragmentVisible(Fragment source) {
		MainActivity mainActivity = getMainActivityOrNull();
		return mainActivity != null && mainActivity.isCurrentFragmentVisible(source);
	}

	public void setABReady(Fragment source, boolean ready, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentReady = ready;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABTitle(Fragment source, CharSequence title, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentTitle = title;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABSubtitle(Fragment source, CharSequence subtitle, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentSubtitle = subtitle;
		if (update) {
			updateABDrawerClosed();
		}
	}


	public void setABBgColor(Fragment source, @ColorInt Integer bgColor, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentBgColor = bgColor;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABCustomView(Fragment source, View customView, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomView = customView;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABCustomViewFocusable(Fragment source, boolean fragmentCustomViewFocusable, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomViewFocusable = fragmentCustomViewFocusable;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABCustomViewRequestFocus(Fragment source, boolean fragmentCustomViewRequestFocus, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomViewRequestFocus = fragmentCustomViewRequestFocus;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABDisplayHomeAsUpEnabled(Fragment source, boolean displayHomeAsUpEnabled, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		if (update) {
			updateABDrawerClosed();
		}
	}

	public void setABShowSearchMenuItem(Fragment source, boolean showSearchMenuItem, boolean update) {
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
		ActionBar ab = getABOrNull();
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
		MainActivity mainActivity = getMainActivityOrNull();
		if (mainActivity != null) {
			mainActivity.updateNavigationDrawerToggleIndicator();
		}
		updateSearchMenuItemVisibility(); // action bar icons are options menu items
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
		ColorDrawable bgDrawable = getBgDrawableOrNull(ab);
		if (bgDrawable != null) {
			bgDrawable.setColor(colorInt);
		}
	}

	private ColorDrawable getBgDrawableOrNull(ActionBar ab) {
		if (this.bgDrawable == null) {
			initBgDrawable(ab);
		}
		return this.bgDrawable;
	}

	private void initBgDrawable(ActionBar ab) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			this.bgDrawable = new ColorDrawable();
			this.bgDrawable.setCallback(this);
		} else {
			if (ab != null) {
				this.bgDrawable = new ColorDrawable();
				ab.setBackgroundDrawable(this.bgDrawable);
			}
		}
	}

	private UpOnClickListener getUpOnClickListener(MainActivity mainActivity) {
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

	public void onSaveState(Bundle outState) {
	}

	public void onRestoreState(Bundle savedInstanceState) {
	}

	public void destroy() {
		if (this.mainActivityWR != null) {
			this.mainActivityWR.clear();
			this.mainActivityWR = null;
		}
		this.fragmentCustomView = null;
		this.upOnClickListener = null;
	}

	public boolean onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.menu_main, menu);
		this.searchMenuItem = menu.findItem(R.id.menu_search);
		updateSearchMenuItemVisibility();
		return true;
	}

	private MenuItem searchMenuItem;

	public void updateSearchMenuItemVisibility() {
		if (this.searchMenuItem != null) {
			this.searchMenuItem.setVisible(this.fragmentShowSearchMenuItem);
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		MainActivity mainActivity = getMainActivityOrNull();
		if (mainActivity != null) {
			if (item.getItemId() == android.R.id.home) {
				if (mainActivity.onUpIconClick()) {
					return true; // handled
				}
			}
			if (item.getItemId() == R.id.menu_search) {
				mainActivity.onSearchRequested();
				return true; // handled
			}
		}
		return false; // not handled
	}

	private static class UpOnClickListener extends MTOnClickListener {

		private WeakReference<MainActivity> mainActivityWR;

		public UpOnClickListener(MainActivity mainActivity) {
			this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
		}

		@Override
		public void onClickMT(View view) {
			MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				mainActivity.onUpIconClick();
			}
		}
	}

	public interface ActionBarColorizer {
		int getBgColor(int position);
	}

	public static class SimpleActionBarColorizer implements ActionBarColorizer {

		private int[] bgColors;

		@Override
		public final int getBgColor(int position) {
			return bgColors[position % bgColors.length];
		}

		public void setBgColors(int... colors) {
			bgColors = colors;
		}
	}
}
