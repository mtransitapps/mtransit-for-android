package org.mtransit.android.ui;

import java.lang.ref.WeakReference;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.MenuAdapter;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.fragment.ABFragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;

@SuppressWarnings("deprecation")
// need to switch to support-v7-appcompat
public class NavigationDrawerController implements MTLog.Loggable, MenuAdapter.MenuUpdateListener, ListView.OnItemClickListener {

	private static final String TAG = NavigationDrawerController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private WeakReference<MainActivity> mainActivityWR;

	private DrawerLayout drawerLayout;
	private ABDrawerToggle drawerToggle;

	private View leftDrawer;
	private ListView drawerListView;
	private MenuAdapter drawerListViewAdapter;

	private int currentSelectedItemPosition = -1;

	private String currentSelectedScreenItemId = null;

	public NavigationDrawerController(MainActivity mainActivity) {
		this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
	}

	public void setup() {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			this.leftDrawer = mainActivity.findViewById(R.id.left_drawer);
			this.drawerListView = (ListView) mainActivity.findViewById(R.id.left_drawer_list);
			this.drawerListView.setOnItemClickListener(this);
			showDrawerLoading();
			this.drawerLayout = (DrawerLayout) mainActivity.findViewById(R.id.drawer_layout);
			this.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
			this.drawerToggle = new ABDrawerToggle(mainActivity, this.drawerLayout);
			this.drawerLayout.setDrawerListener(drawerToggle);
			finishSetupAsync();
		}
	}

	private void finishSetupAsync() {
		new MTAsyncTask<Void, Void, String>() {

			private final String TAG = NavigationDrawerController.TAG + ">finishSetupAsync()";

			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected String doInBackgroundMT(Void... params) {
				final MainActivity mainActivity = NavigationDrawerController.this.mainActivityWR == null ? null
						: NavigationDrawerController.this.mainActivityWR.get();
				NavigationDrawerController.this.drawerListViewAdapter = new MenuAdapter(mainActivity, NavigationDrawerController.this);
				NavigationDrawerController.this.drawerListViewAdapter.init();
				if (!isCurrentSelectedSet()) {
					final String itemId = PreferenceUtils.getPrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID,
							MenuAdapter.ITEM_ID_SELECTED_SCREEN_DEFAULT);
					return itemId;
				}
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				NavigationDrawerController.this.drawerListView.setAdapter(NavigationDrawerController.this.drawerListViewAdapter);
				showDrawerLoaded();
				if (!TextUtils.isEmpty(result)) {
					selectItem(NavigationDrawerController.this.drawerListViewAdapter.getScreenItemPosition(result), null, false);
				}
			}
		}.execute();
	}

	@Override
	public void onMenuUpdated() {
		if (this.drawerListViewAdapter == null) {
			return;
		}
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		final String itemId = PreferenceUtils.getPrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID,
				MenuAdapter.ITEM_ID_SELECTED_SCREEN_DEFAULT);
		final int newSelectedItemPosition = this.drawerListViewAdapter.getScreenItemPosition(itemId);
		if (this.currentSelectedScreenItemId != null && this.currentSelectedScreenItemId.equals(itemId)) {
			this.currentSelectedItemPosition = newSelectedItemPosition;
			setCurrentSelectedItemChecked(mainActivity.getBackStackEntryCount() == 0);
			return;
		}
		selectItem(newSelectedItemPosition, null, false); // re-select, selected item
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		selectItem(position, null, false);
	}

	private void selectItem(int position, ABFragment newFragmentOrNull, boolean addToStack) {
		if (position < 0) {
			return;
		}
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		if (position == this.currentSelectedItemPosition) {
			closeDrawer();
			mainActivity.clearFragmentBackStackImmediate();
			return;
		}
		if (!this.drawerListViewAdapter.isRootScreen(position)) {
			setCurrentSelectedItemChecked(true); // keep current position
			return;
		}
		final ABFragment newFragment = newFragmentOrNull != null ? newFragmentOrNull : this.drawerListViewAdapter.getNewStaticFragmentAt(position);
		if (newFragment == null) {
			return;
		}
		this.currentSelectedItemPosition = position;
		this.currentSelectedScreenItemId = this.drawerListViewAdapter.getScreenItemId(position);
		closeDrawer();
		mainActivity.clearFragmentBackStackImmediate(); // root screen
		StatusLoader.get().clearAllTasks();
		mainActivity.showNewFragment(newFragment, addToStack);
		if (!addToStack && this.drawerListViewAdapter.isRootScreen(position)) {
			PreferenceUtils.savePrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, this.currentSelectedScreenItemId, false);
		}
	}

	private void showDrawerLoading() {
		this.drawerListView.setVisibility(View.GONE);
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			if (mainActivity.findViewById(R.id.left_drawer_loading) == null) { // IF NOT present/inflated DO
				((ViewStub) mainActivity.findViewById(R.id.left_drawer_loading_stub)).inflate(); // inflate
			}
			mainActivity.findViewById(R.id.left_drawer_loading).setVisibility(View.VISIBLE);
		}
	}

	public void openDrawer() {
		if (this.drawerLayout != null && this.leftDrawer != null) {
			this.drawerLayout.openDrawer(this.leftDrawer);
		}
	}

	public void closeDrawer() {
		if (this.drawerLayout != null && this.leftDrawer != null) {
			this.drawerLayout.closeDrawer(this.leftDrawer);
		}
	}

	public boolean isDrawerOpen() {
		return this.drawerLayout == null || this.leftDrawer == null ? false : this.drawerLayout.isDrawerOpen(this.leftDrawer);
	}

	public boolean onBackPressed() {
		if (isDrawerOpen()) {
			closeDrawer();
			return true; // processed
		}
		return false; // not processed
	}

	public void setDrawerToggleIndicatorEnabled(boolean enabled) {
		if (this.drawerToggle != null) {
			this.drawerToggle.setDrawerIndicatorEnabled(enabled);
		}
	}

	public void syncDrawerToggleState() {
		if (this.drawerToggle != null) {
			this.drawerToggle.syncState();
		}
	}

	public void onActivityPostCreate(Bundle savedInstanceState) {
		syncDrawerToggleState();
	}

	public void onDrawerToggleConfigurationChanged(Configuration newConfig) {
		if (this.drawerToggle != null) {
			this.drawerToggle.onConfigurationChanged(newConfig);
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		onDrawerToggleConfigurationChanged(newConfig);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (this.drawerToggle != null && this.drawerToggle.onOptionsItemSelected(item)) {
			return true; // processed
		}
		return false;
	}


	private boolean isCurrentSelectedSet() {
		return this.currentSelectedItemPosition >= 0 && this.currentSelectedScreenItemId != null;
	}

	public void setCurrentSelectedItemChecked(boolean value) {
		if (this.drawerListView != null && this.currentSelectedItemPosition >= 0) {
			this.drawerListView.setItemChecked(this.currentSelectedItemPosition, value);
		}
	}

	public void onBackStackChanged(int backStackEntryCount) {
		setCurrentSelectedItemChecked(backStackEntryCount == 0);
	}

	private static final String EXTRA_SELECTED_ROOT_SCREEN_POSITION = "extra_selected_root_screen";
	private static final String EXTRA_SELECTED_ROOT_SCREEN_ID = "extra_selected_root_screen_id";

	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(EXTRA_SELECTED_ROOT_SCREEN_POSITION, this.currentSelectedItemPosition);
		outState.putString(EXTRA_SELECTED_ROOT_SCREEN_ID, this.currentSelectedScreenItemId);
	}

	public void onRestoreState(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			return; // nothing to restore
		}
		int savedRootScreen = savedInstanceState.getInt(EXTRA_SELECTED_ROOT_SCREEN_POSITION, -1);
		if (savedRootScreen >= 0) {
			this.currentSelectedItemPosition = savedRootScreen;
		}
		String savedRootScreenId = savedInstanceState.getString(EXTRA_SELECTED_ROOT_SCREEN_ID, null);
		if (!TextUtils.isEmpty(savedRootScreenId)) {
			this.currentSelectedScreenItemId = savedRootScreenId;
		}
	}

	private void showDrawerLoaded() {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			if (mainActivity.findViewById(R.id.left_drawer_loading) != null) {
				mainActivity.findViewById(R.id.left_drawer_loading).setVisibility(View.GONE);
			}
		}
		/* findViewById(R.id.left_drawer_list) */
		this.drawerListView.setVisibility(View.VISIBLE);
	}

	public void destroy() {
		if (this.mainActivityWR != null) {
			this.mainActivityWR.clear();
			this.mainActivityWR = null;
		}
		this.leftDrawer = null;
		this.drawerListView = null;
		this.drawerToggle = null;
	}

	private static class ABDrawerToggle extends ActionBarDrawerToggle implements MTLog.Loggable {

		private static final String TAG = MainActivity.class.getSimpleName() + ">" + ABDrawerToggle.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<MainActivity> mainActivityWR;

		public ABDrawerToggle(MainActivity mainActivity, DrawerLayout drawerLayout) {
			super(mainActivity, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
			this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
		}

		@Override
		public void onDrawerClosed(View view) {
			final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				mainActivity.updateAB();
			}
		}

		@Override
		public void onDrawerOpened(View drawerView) {
			final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				mainActivity.updateAB();
			}
		}

	}

}
