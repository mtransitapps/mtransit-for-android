package org.mtransit.android.ui;

import java.lang.ref.WeakReference;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.ABFragment;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class ActionBarController implements MTLog.Loggable {

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

	private int drawerIcon;
	private int fragmentIcon;

	private Integer fragmentBgColor;
	private Integer drawerBgColor;

	private View fragmentCustomView;
	private View drawerCustomView;

	private boolean fragmentThemeDarkInsteadOfThemeLight;

	private boolean fragmentDisplayHomeAsUpEnabled;
	private boolean drawerDisplayHomeAsUpEnabled;

	private boolean fragmentShowSearchMenuItem;


	private UpOnClickListener upOnClickListener;

	public ActionBarController(MainActivity mainActivity) {
		setMainActivity(mainActivity);
		init();
	}

	public void setMainActivity(MainActivity mainActivity) {
		this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
	}

	private Context getContextOrNull() {
		return getMainActivityOrNull();
	}

	private MainActivity getMainActivityOrNull() {
		return this.mainActivityWR == null ? null : this.mainActivityWR.get();
	}

	private ActionBar getABOrNull() {
		final MainActivity mainActivity = getMainActivityOrNull();
		return mainActivity == null ? null : mainActivity.getActionBar();
	}
	private void init() {
		final MainActivity mainActivity = getMainActivityOrNull();
		if (mainActivity != null) {
			final ActionBar ab = getABOrNull();
			ab.hide();
			this.fragmentReady = false;
			this.fragmentTitle = this.drawerTitle = mainActivity.getTitle();
			this.fragmentSubtitle = this.drawerSubtitle = ab.getSubtitle();
			this.fragmentIcon = this.drawerIcon = R.mipmap.ic_launcher;
			this.fragmentBgColor = this.drawerBgColor = ABFragment.NO_BG_COLOR;
			this.fragmentCustomView = this.drawerCustomView = ABFragment.NO_CUSTOM_VIEW;
			this.fragmentThemeDarkInsteadOfThemeLight = ABFragment.DEFAULT_THEME_DARK_INSTEAD_OF_LIGHT;
			this.fragmentDisplayHomeAsUpEnabled = this.drawerDisplayHomeAsUpEnabled = ABFragment.DEFAULT_DISPLAY_HOME_AS_UP_ENABLED;
			this.fragmentShowSearchMenuItem = ABFragment.DEFAULT_SHOW_SEARCH_MENU_ITEM;
			ab.setDisplayHomeAsUpEnabled(this.fragmentDisplayHomeAsUpEnabled);
			ab.setHomeButtonEnabled(true);
		}
	}

	public void setAB(ABFragment abf) {
		final Context context = getContextOrNull();
		if (context != null) {
			setAB(abf.getABTitle(context), abf.getABSubtitle(context), abf.getABIconDrawableResId(), abf.getABBgColor(), abf.getABCustomView(),
					abf.isABThemeDarkInsteadOfThemeLight(), abf.isABDisplayHomeAsUpEnabled(), abf.isABShowSearchMenuItem(), abf.isABReady());
		}
	}

	private void setAB(CharSequence title, CharSequence subtitle, int iconResId, Integer bgColor, View customView, boolean themeDarkInsteadOfThemeLight,
			boolean displayHomeAsUpEnabled, boolean showSearchMenuItem, boolean fragmentReady) {
		this.fragmentTitle = title;
		this.fragmentSubtitle = subtitle;
		this.fragmentIcon = iconResId;
		this.fragmentBgColor = bgColor;
		this.fragmentCustomView = customView;
		this.fragmentThemeDarkInsteadOfThemeLight = themeDarkInsteadOfThemeLight;
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
			updateAB();
		}
	}

	public void setABTitle(Fragment source, CharSequence title, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentTitle = title;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABSubtitle(Fragment source, CharSequence subtitle, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentSubtitle = subtitle;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABIcon(Fragment source, int resId, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentIcon = resId;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABBgColor(Fragment source, int bgColor, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentBgColor = bgColor;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABCustomView(Fragment source, View customView, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentCustomView = customView;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABDisplayHomeAsUpEnabled(Fragment source, boolean displayHomeAsUpEnabled, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABThemeDarkInsteadOfThemeLight(Fragment source, boolean themeDarkInsteadOfThemeLight, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentThemeDarkInsteadOfThemeLight = themeDarkInsteadOfThemeLight;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABShowSearchMenuItem(Fragment source, boolean showSearchMenuItem, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		this.fragmentShowSearchMenuItem = showSearchMenuItem;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void updateAB() {
		if (isDrawerOpen()) {
			updateABDrawerOpened();
		} else {
			updateABDrawerClosed();
		}
	}


	private void updateABDrawerClosed() {
		final MainActivity mainActivity = getMainActivityOrNull();
		final ActionBar ab = getABOrNull();
		if (!this.fragmentReady) {
			return;
		}
		if (this.fragmentCustomView != null) {
			ab.setCustomView(this.fragmentCustomView);
			if (!this.fragmentDisplayHomeAsUpEnabled) {
				ab.getCustomView().setOnClickListener(getUpOnClickListener(getMainActivityOrNull()));
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
		if (this.fragmentIcon > 0) {
			ab.setIcon(this.fragmentIcon);
			ab.setDisplayShowHomeEnabled(true);
		} else {
			ab.setDisplayShowHomeEnabled(false);
		}
		if (this.fragmentBgColor != null) {
			ab.setBackgroundDrawable(new ColorDrawable(this.fragmentBgColor.intValue()));
		} else {
			ab.setBackgroundDrawable(null);
		}
		mainActivity.updateNavigationDrawerToggleIndicator();
		updateAllMenuItems(); // action bar icons are options menu items
		ab.show();
	}

	private void updateABDrawerOpened() {
		final MainActivity mainActivity = getMainActivityOrNull();
		final ActionBar ab = getABOrNull();
		if (this.drawerCustomView != null) {
			ab.setCustomView(this.drawerCustomView);
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
		if (this.drawerIcon > 0) {
			ab.setIcon(this.drawerIcon);
			ab.setDisplayShowHomeEnabled(true);
		} else {
			ab.setDisplayShowHomeEnabled(false);
		}
		if (this.drawerBgColor != null) {
			ab.setBackgroundDrawable(new ColorDrawable(this.drawerBgColor));
		} else {
			ab.setBackgroundDrawable(null);
		}
		mainActivity.enableNavigationDrawerToggleIndicator();
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
					this.allMenuItems.get(R.id.menu_search).setIcon(
							this.fragmentThemeDarkInsteadOfThemeLight ? R.drawable.ic_menu_action_search_holo_dark
									: R.drawable.ic_menu_action_search_holo_light);
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

}
