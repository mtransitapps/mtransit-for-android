package org.mtransit.android.ui;

import java.lang.ref.WeakReference;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.ABFragment;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class ActionBarController implements Drawable.Callback, MTLog.Loggable {

	private static final String TAG = ActionBarController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}


	private WeakReference<MainActivity> mainActivityWR;

	private boolean fragmentReady;

	private CharSequence drawerTitle;
	private CharSequence fragmentTitle;

	private CharSequence drawerSubtitle;
	private CharSequence fragmentSubtitle;


	private Integer fragmentBgColor = null;

	private View fragmentCustomView = null;
	private View drawerCustomView = null;

	private boolean fragmentCustomViewRequestFocus = false;


	private boolean fragmentDisplayHomeAsUpEnabled;
	private boolean drawerDisplayHomeAsUpEnabled;

	private boolean fragmentShowSearchMenuItem;


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
		final MainActivity mainActivity = getMainActivityOrNull();
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
		final MainActivity mainActivity = getMainActivityOrNull();
		if (mainActivity != null) {
			ActionBar ab = getABOrNull();
			this.fragmentReady = false;
			this.fragmentTitle = this.drawerTitle = mainActivity.getTitle();
			this.fragmentSubtitle = this.drawerSubtitle = ab == null ? null : ab.getSubtitle();
			this.fragmentDisplayHomeAsUpEnabled = this.drawerDisplayHomeAsUpEnabled = ABFragment.DEFAULT_DISPLAY_HOME_AS_UP_ENABLED;
			this.fragmentShowSearchMenuItem = ABFragment.DEFAULT_SHOW_SEARCH_MENU_ITEM;
			if (ab != null) {
				ab.setElevation(0f);
				ab.hide();
				ab.setDisplayHomeAsUpEnabled(this.fragmentDisplayHomeAsUpEnabled);
				ab.setHomeButtonEnabled(true);
			}
			initBgDrawable(ab);
		}
	}

	public void setAB(ABFragment abf) {
		final Context context = getContextOrNull();
		if (abf != null && context != null) {
			setAB(abf.getABTitle(context), abf.getABSubtitle(context), abf.getABBgColor(context), abf.getABCustomView(), abf.isABCustomViewRequestFocus(),
					abf.isABThemeDarkInsteadOfThemeLight(), abf.isABDisplayHomeAsUpEnabled(), abf.isABShowSearchMenuItem(), abf.isABReady());
		}
	}

	private void setAB(CharSequence title, CharSequence subtitle, Integer bgColor, View customView, boolean customViewRequestFocus,
			boolean themeDarkInsteadOfThemeLight, boolean displayHomeAsUpEnabled, boolean showSearchMenuItem, boolean fragmentReady) {
		this.fragmentTitle = title;
		this.fragmentSubtitle = subtitle;
		this.fragmentBgColor = bgColor;
		this.fragmentCustomView = customView;
		this.fragmentCustomViewRequestFocus = customViewRequestFocus;
		this.fragmentDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		this.fragmentShowSearchMenuItem = showSearchMenuItem;
		this.fragmentReady = fragmentReady;
	}

	private boolean isCurrentFragmentVisible(Fragment source) {
		final MainActivity mainActivity = getMainActivityOrNull();
		return mainActivity == null ? false : mainActivity.isCurrentFragmentVisible(source);
	}

	private boolean isDrawerOpen() {
		final MainActivity mainActivity = getMainActivityOrNull();
		return mainActivity == null ? false : mainActivity.isDrawerOpen();
	}

	public void setABReady(Fragment source, boolean ready, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentReady = ready;
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}

	public void setABTitle(Fragment source, CharSequence title, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentTitle = title;
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}

	public void setABSubtitle(Fragment source, CharSequence subtitle, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentSubtitle = subtitle;
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}

	public void setABIcon(Fragment source, int resId, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}

	public void setABBgColor(Fragment source, Integer bgColor, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentBgColor = bgColor;
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}

	public void setABCustomView(Fragment source, View customView, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomView = customView;
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}

	public void setABCustomViewRequestFocus(Fragment source, boolean fragmentCustomViewRequestFocus, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomViewRequestFocus = fragmentCustomViewRequestFocus;
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}

	public void setABDisplayHomeAsUpEnabled(Fragment source, boolean displayHomeAsUpEnabled, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}


	public void setABShowSearchMenuItem(Fragment source, boolean showSearchMenuItem, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentShowSearchMenuItem = showSearchMenuItem;
		if (update && !isDrawerOpen()) {
			updateABDrawerClosed();
		}
	}

	public void updateAB() {
		if (isDrawerOpen()) {
			updateABDrawerOpened();
		} else {
			updateABDrawerClosed();
		}
	}


	public void updateABDrawerClosed() {
		final MainActivity mainActivity = getMainActivityOrNull();
		final ActionBar ab = getABOrNull();
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
			if (this.fragmentCustomViewRequestFocus) {
				this.fragmentCustomView.setFocusable(true);
				this.fragmentCustomView.requestFocusFromTouch();
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
			setBgColor(ab, this.fragmentBgColor.intValue());
		}
		mainActivity.updateNavigationDrawerToggleIndicator();
		updateAllMenuItems(); // action bar icons are options menu items
		ab.show();
	}

	public void updateABBgColor() {
		if (!this.fragmentReady) {
			return;
		}
		if (this.fragmentBgColor != null) {
			setBgColor(getABOrNull(), this.fragmentBgColor.intValue());
		}
	}

	private void setBgColor(ActionBar ab, int colorInt) {
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

	public void updateABDrawerOpened() {
		final MainActivity mainActivity = getMainActivityOrNull();
		final ActionBar ab = getABOrNull();
		if (this.drawerCustomView != null) {
			if (!this.drawerCustomView.equals(ab.getCustomView())) {
				ab.setCustomView(this.drawerCustomView);
			}
			if (!this.drawerDisplayHomeAsUpEnabled) {
				ab.getCustomView().setOnClickListener(getUpOnClickListener(mainActivity));
			}
			ab.setDisplayShowCustomEnabled(true);
		} else {
			ab.setDisplayShowCustomEnabled(false);
		}
		ab.setDisplayHomeAsUpEnabled(this.drawerDisplayHomeAsUpEnabled);
		if (TextUtils.isEmpty(this.drawerTitle)) {
			ab.setDisplayShowTitleEnabled(false);
		} else {
			ab.setTitle(this.drawerTitle);
			ab.setSubtitle(this.drawerSubtitle);
			ab.setDisplayShowTitleEnabled(true);
		}
		updateAllMenuItems(); // action bar icons are options menu items
		ab.show();
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

	private SparseArray<MenuItem> allMenuItems = new SparseArray<MenuItem>();

	public void addMenuItem(int resId, MenuItem menuItem) {
		this.allMenuItems.put(resId, menuItem);
	}

	public MenuItem getMenuItem(int resId) {
		return this.allMenuItems.get(resId);
	}


	public void destroy() {
		if (this.mainActivityWR != null) {
			this.mainActivityWR.clear();
			this.mainActivityWR = null;
		}
		if (this.allMenuItems != null) {
			this.allMenuItems.clear();
		}
		this.fragmentCustomView = null;
		this.upOnClickListener = null;
		this.drawerCustomView = null;
	}

	public boolean onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.main_activity, menu);
		initMenuItems(menu);
		return true;
	}

	public void initMenuItems(Menu menu) {
		this.allMenuItems.clear();
		this.allMenuItems.put(R.id.menu_search, menu.findItem(R.id.menu_search));
		updateAllMenuItems();
	}

	public void updateAllMenuItems() {
		final boolean drawerOpen = isDrawerOpen();
		final boolean showABIcons = !drawerOpen;

		if (this.allMenuItems != null) {
			for (int i = 0; i < this.allMenuItems.size(); i++) {
				final int menuItemId = this.allMenuItems.keyAt(i);
				final MenuItem menuItem = this.allMenuItems.get(menuItemId);
				if (menuItemId == R.id.menu_search) {
					menuItem.setVisible(this.fragmentShowSearchMenuItem && showABIcons);
					continue;
				}
				menuItem.setVisible(showABIcons);
			}
		}

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		final MainActivity mainActivity = getMainActivityOrNull();
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

	private static class UpOnClickListener implements View.OnClickListener {

		private WeakReference<MainActivity> mainActivityWR;

		public UpOnClickListener(MainActivity mainActivity) {
			this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
		}

		@Override
		public void onClick(View v) {
			final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				mainActivity.onUpIconClick();
			}
		}
	}

	public static interface ActionBarColorizer {

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
