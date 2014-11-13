package org.mtransit.android.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.ABFragment;

import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ActionBarController implements MTLog.Loggable {

	private static final String TAG = ActionBarController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final long MIN_DURATION_BETWEEN_UPDATE_AB_IN_MS = 100l; // 0.1 second

	private WeakReference<MainActivity> mainActivityWR;

	private CharSequence drawerTitle;
	private CharSequence fragmentTitle;

	private CharSequence drawerSubtitle;
	private CharSequence fragmentSubtitle;

	private int fragmentIcon;
	private int drawerIcon;

	private Integer fragmentBgColor;
	private Integer drawerBgColor;

	private View fragmentCustomView;
	private View drawerCustomView;

	private boolean fragmentThemeDarkInsteadOfThemeLight;

	private boolean fragmentDisplayHomeAsUpEnabled;
	private boolean drawerDisplayHomeAsUpEnabled;

	private boolean fragmentShowSearchMenuItem;

	private Handler handler = new Handler();

	private long lastUpdateAB = -1l;

	private UpdateABLater updateABLater = null;

	private UpOnClickListener upOnClickListener;

	public ActionBarController(MainActivity mainActivity) {
		this.mainActivityWR = new WeakReference<>(mainActivity);
		init();
	}

	private void init() {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			this.fragmentTitle = this.drawerTitle = mainActivity.getTitle();
			this.fragmentSubtitle = this.drawerSubtitle = mainActivity.getActionBar().getSubtitle();
			this.fragmentIcon = this.drawerIcon = R.mipmap.ic_launcher;
			this.fragmentBgColor = this.drawerBgColor = ABFragment.NO_BG_COLOR;
			this.fragmentCustomView = this.drawerCustomView = mainActivity.getActionBar().getCustomView();
			this.fragmentThemeDarkInsteadOfThemeLight = false;
			this.fragmentDisplayHomeAsUpEnabled = this.drawerDisplayHomeAsUpEnabled = true;
			this.fragmentShowSearchMenuItem = true;

			mainActivity.getActionBar().setDisplayHomeAsUpEnabled(true);
			mainActivity.getActionBar().setHomeButtonEnabled(true);
		}
	}

	public void setAB(ABFragment abf) {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			setAB(abf.getABTitle(mainActivity), abf.getABSubtitle(mainActivity), abf.getABIconDrawableResId(), abf.getABBgColor(), abf.getABCustomView(),
					abf.isABThemeDarkInsteadOfThemeLight(), abf.isABDisplayHomeAsUpEnabled(), abf.isABShowSearchMenuItem());
		}
	}

	private void setAB(CharSequence title, CharSequence subtitle, int iconResId, Integer bgColor, View customView, boolean themeDarkInsteadOfThemeLight,
			boolean displayHomeAsUpEnabled, boolean showSearchMenuItem) {
		fragmentTitle = title;
		fragmentSubtitle = subtitle;
		fragmentIcon = iconResId;
		fragmentBgColor = bgColor;
		fragmentCustomView = customView;
		fragmentThemeDarkInsteadOfThemeLight = themeDarkInsteadOfThemeLight;
		fragmentDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		fragmentShowSearchMenuItem = showSearchMenuItem;
	}

	private boolean isCurrentFragmentVisible(Fragment source) {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		return mainActivity == null ? false : mainActivity.isCurrentFragmentVisible(source);
	}

	private boolean isDrawerOpen() {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		return mainActivity == null ? false : mainActivity.isDrawerOpen();
	}

	public void setABTitle(Fragment source, CharSequence title, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		fragmentTitle = title;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABSubtitle(Fragment source, CharSequence subtitle, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		fragmentSubtitle = subtitle;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABIcon(Fragment source, int resId, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		fragmentIcon = resId;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABBgColor(Fragment source, int bgColor, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		fragmentBgColor = bgColor;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABCustomView(Fragment source, View customView, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		fragmentCustomView = customView;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABDisplayHomeAsUpEnabled(Fragment source, boolean displayHomeAsUpEnabled, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		fragmentDisplayHomeAsUpEnabled = displayHomeAsUpEnabled;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABThemeDarkInsteadOfThemeLight(Fragment source, boolean themeDarkInsteadOfThemeLight, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		fragmentThemeDarkInsteadOfThemeLight = themeDarkInsteadOfThemeLight;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void setABShowSearchMenuItem(Fragment source, boolean showSearchMenuItem, boolean update) {
		if (!isCurrentFragmentVisible(source)) {
			return;
		}
		fragmentShowSearchMenuItem = showSearchMenuItem;
		if (update && !isDrawerOpen()) {
			updateAB();
		}
	}

	public void updateAB() {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		final long now = System.currentTimeMillis();
		final long howLongBeforeNextUpdateABInMs = this.lastUpdateAB + MIN_DURATION_BETWEEN_UPDATE_AB_IN_MS - now;
		if (mainActivity.isNotDrawerState(DrawerLayout.STATE_IDLE) || howLongBeforeNextUpdateABInMs > 0) {
			if (this.updateABLater == null) {
				this.updateABLater = new UpdateABLater(this);
				this.handler.postDelayed(this.updateABLater, howLongBeforeNextUpdateABInMs > 0 ? howLongBeforeNextUpdateABInMs
						: MIN_DURATION_BETWEEN_UPDATE_AB_IN_MS);
			}
			return;
		}
		updateLaterDone();
		if (mainActivity.isDrawerOpen()) {
			updateABDrawerOpened(mainActivity);
		} else {
			updateABDrawerClosed(mainActivity);
		}
		this.lastUpdateAB = now;
	}

	private void updateLaterDone() {
		if (this.updateABLater != null) {
			this.handler.removeCallbacks(this.updateABLater);
			this.updateABLater = null;
		}
	}

	private void updateABDrawerClosed(MainActivity mainActivity) {
		if (fragmentCustomView != null) {
			mainActivity.getActionBar().setCustomView(fragmentCustomView);
			if (!fragmentDisplayHomeAsUpEnabled) {
				mainActivity.getActionBar().getCustomView().setOnClickListener(getUpOnClickListener(mainActivity));
			}
			mainActivity.getActionBar().setDisplayShowCustomEnabled(true);
		} else {
			mainActivity.getActionBar().setDisplayShowCustomEnabled(false);
		}
		mainActivity.getActionBar().setDisplayHomeAsUpEnabled(fragmentDisplayHomeAsUpEnabled);
		if (TextUtils.isEmpty(fragmentTitle)) {
			mainActivity.getActionBar().setDisplayShowTitleEnabled(false);
		} else {
			mainActivity.getActionBar().setTitle(fragmentTitle);
			mainActivity.getActionBar().setSubtitle(fragmentSubtitle);
			mainActivity.getActionBar().setDisplayShowTitleEnabled(true);
		}
		if (fragmentIcon > 0) {
			mainActivity.getActionBar().setIcon(fragmentIcon);
			mainActivity.getActionBar().setDisplayShowHomeEnabled(true);
		} else {
			mainActivity.getActionBar().setDisplayShowHomeEnabled(false);
		}
		if (fragmentBgColor != null) {
			mainActivity.getActionBar().setBackgroundDrawable(new ColorDrawable(fragmentBgColor));
		} else {
			mainActivity.getActionBar().setBackgroundDrawable(null);
		}
		mainActivity.updateNavigationDrawerToggleIndicator();
		updateAllMenuItems(); // action bar icons are options menu items
	}

	private void updateABDrawerOpened(MainActivity mainActivity) {
		if (drawerCustomView != null) {
			mainActivity.getActionBar().setCustomView(drawerCustomView);
			if (!drawerDisplayHomeAsUpEnabled) {
				mainActivity.getActionBar().getCustomView().setOnClickListener(getUpOnClickListener(mainActivity));
			}
			mainActivity.getActionBar().setDisplayShowCustomEnabled(true);
		} else {
			mainActivity.getActionBar().setDisplayShowCustomEnabled(false);
		}
		mainActivity.getActionBar().setDisplayHomeAsUpEnabled(drawerDisplayHomeAsUpEnabled);
		if (TextUtils.isEmpty(drawerTitle)) {
			mainActivity.getActionBar().setDisplayShowTitleEnabled(false);
		} else {
			mainActivity.getActionBar().setTitle(drawerTitle);
			mainActivity.getActionBar().setSubtitle(drawerSubtitle);
			mainActivity.getActionBar().setDisplayShowTitleEnabled(true);
		}
		if (drawerIcon > 0) {
			mainActivity.getActionBar().setIcon(drawerIcon);
			mainActivity.getActionBar().setDisplayShowHomeEnabled(true);
		} else {
			mainActivity.getActionBar().setDisplayShowHomeEnabled(false);
		}
		if (drawerBgColor != null) {
			mainActivity.getActionBar().setBackgroundDrawable(new ColorDrawable(drawerBgColor));
		} else {
			mainActivity.getActionBar().setBackgroundDrawable(null);
		}
		mainActivity.enableNavigationDrawerToggleIndicator();
		updateAllMenuItems(); // action bar icons are options menu items
	}

	private UpOnClickListener getUpOnClickListener(MainActivity mainActivity) {
		if (this.upOnClickListener == null) {
			if (mainActivity == null) {
				mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			}
			if (mainActivity != null) {
				this.upOnClickListener = new UpOnClickListener(mainActivity);
			}
		}
		return this.upOnClickListener;
	}

	private HashMap<Integer, MenuItem> allMenuItems = new HashMap<Integer, MenuItem>();

	public void addMenuItem(int resId, MenuItem menuItem) {
		this.allMenuItems.put(resId, menuItem);
	}

	public MenuItem getMenuItem(int resId) {
		return this.allMenuItems.get(resId);
	}

	public void initMenuItems(Menu menu) {
		this.allMenuItems.clear();
		this.allMenuItems.put(R.id.menu_search, menu.findItem(R.id.menu_search));
		final MenuItem menuToggleListGrid = menu.findItem(R.id.menu_toggle_list_grid);
		if (menuToggleListGrid != null) {
			this.allMenuItems.put(R.id.menu_toggle_list_grid, menuToggleListGrid);
		}
		final MenuItem menuAddRemoveFavorite = menu.findItem(R.id.menu_add_remove_favorite);
		if (menuAddRemoveFavorite != null) {
			this.allMenuItems.put(R.id.menu_add_remove_favorite, menuAddRemoveFavorite);
		}
		updateAllMenuItems();
	}

	public void updateAllMenuItems() {
		if (this.allMenuItems == null || this.allMenuItems.size() == 0) {
			return;
		}
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		final boolean drawerOpen = mainActivity.isDrawerOpen();
		final boolean showABIcons = !drawerOpen;
		if (this.allMenuItems.get(R.id.menu_toggle_list_grid) != null) {
			this.allMenuItems.get(R.id.menu_toggle_list_grid).setVisible(showABIcons);
		}
		if (this.allMenuItems.get(R.id.menu_add_remove_favorite) != null) {
			this.allMenuItems.get(R.id.menu_add_remove_favorite).setVisible(showABIcons);
		}
		this.allMenuItems.get(R.id.menu_search).setVisible(this.fragmentShowSearchMenuItem && showABIcons);
		this.allMenuItems.get(R.id.menu_search).setIcon(
				fragmentThemeDarkInsteadOfThemeLight ? R.drawable.ic_menu_action_search_holo_dark : R.drawable.ic_menu_action_search_holo_light);
	}

	public void resetLastUpdateTime() {
		this.lastUpdateAB = -1;
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
		this.drawerCustomView = null;
	}

	private static class UpdateABLater implements Runnable {

		private WeakReference<ActionBarController> abControllerWR;

		public UpdateABLater(ActionBarController abController) {
			this.abControllerWR = new WeakReference<ActionBarController>(abController);
		}

		@Override
		public void run() {
			final ActionBarController abController = this.abControllerWR == null ? null : this.abControllerWR.get();
			if (abController != null) {
				abController.updateLaterDone();
				abController.updateAB();
			}
		}
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
