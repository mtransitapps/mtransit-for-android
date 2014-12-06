package org.mtransit.android.ui;

import java.lang.ref.WeakReference;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.MenuAdapter;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.fragment.ABFragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;

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

	public void setup(Bundle savedInstanceState) {
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			this.leftDrawer = mainActivity.findViewById(R.id.left_drawer);
			this.drawerListView = (ListView) mainActivity.findViewById(R.id.left_drawer_list);
			this.drawerListView.setOnItemClickListener(this);
			showDrawerLoading();
			this.drawerLayout = (DrawerLayout) mainActivity.findViewById(R.id.drawer_layout);
			this.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
			this.drawerToggle = new ABDrawerToggle(mainActivity, this.drawerLayout);
			this.drawerLayout.setDrawerListener(drawerToggle);
			finishSetupAsync(savedInstanceState);
		}
	}

	private void finishSetupAsync(Bundle savedInstanceState) {
		new MTAsyncTask<Bundle, String, String>() {

			private final String TAG = NavigationDrawerController.TAG + ">finishSetupAsync()";

			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected String doInBackgroundMT(Bundle... params) {
				Bundle savedInstanceState = params == null || params.length == 0 ? null : params[0];
				MainActivity mainActivity = NavigationDrawerController.this.mainActivityWR == null ? null : NavigationDrawerController.this.mainActivityWR
						.get();
				NavigationDrawerController.this.drawerListViewAdapter = new MenuAdapter(mainActivity, NavigationDrawerController.this);
				String itemId = null;
				if (savedInstanceState == null && !isCurrentSelectedSet()) {
					itemId = PreferenceUtils.getPrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID,
							MenuAdapter.ITEM_ID_SELECTED_SCREEN_DEFAULT);
					publishProgress(itemId);
				}
				NavigationDrawerController.this.drawerListViewAdapter.init();
				return itemId;
			}

			@Override
			protected void onPostExecute(String itemId) {
				NavigationDrawerController.this.drawerListView.setAdapter(NavigationDrawerController.this.drawerListViewAdapter);
				showDrawerLoaded();
				selectItemId(itemId);
			}

			public void selectItemId(String itemId) {
				if (!TextUtils.isEmpty(itemId)) {
					int screenItemPosition = NavigationDrawerController.this.drawerListViewAdapter.getScreenItemPosition(itemId);
					selectItem(screenItemPosition);
				}
			}

			@Override
			protected void onProgressUpdate(String... itemIds) {
				super.onProgressUpdate(itemIds);
				selectItemId(itemIds == null || itemIds.length == 0 ? null : itemIds[0]);
			}

		}.execute(savedInstanceState);
	}

	@Override
	public void onMenuUpdated() {
		if (this.drawerListViewAdapter == null) {
			return;
		}
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		String itemId = PreferenceUtils.getPrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, MenuAdapter.ITEM_ID_SELECTED_SCREEN_DEFAULT);
		int newSelectedItemPosition = this.drawerListViewAdapter.getScreenItemPosition(itemId);
		if (this.currentSelectedItemPosition == newSelectedItemPosition && this.currentSelectedScreenItemId != null
				&& this.currentSelectedScreenItemId.equals(itemId)) {
			this.currentSelectedItemPosition = newSelectedItemPosition;
			setCurrentSelectedItemChecked(mainActivity.getBackStackEntryCount() == 0);
			return;
		}
		selectItem(newSelectedItemPosition);
	}

	public void forceReset() {
		if (this.currentSelectedItemPosition < 0) {
			return;
		}
		int saveCurrentSelectedItemPosition = this.currentSelectedItemPosition;
		this.currentSelectedItemPosition = -1;
		this.currentSelectedScreenItemId = null;
		selectItem(saveCurrentSelectedItemPosition);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		selectItem(position);
	}

	private void selectItem(int position) {
		if (position < 0) {
			return;
		}
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity == null) {
			return;
		}
		if (position == this.currentSelectedItemPosition) {
			closeDrawer();
			mainActivity.clearFragmentBackStackImmediate();
			setCurrentSelectedItemChecked(true);
			mainActivity.showContentFrameAsLoaded();
			return;
		}
		if (!this.drawerListViewAdapter.isRootScreen(position)) {
			setCurrentSelectedItemChecked(true); // keep current position
			return;
		}
		ABFragment newFragment = this.drawerListViewAdapter.getNewStaticFragmentAt(position);
		if (newFragment == null) {
			return;
		}
		this.currentSelectedItemPosition = position;
		this.currentSelectedScreenItemId = this.drawerListViewAdapter.getScreenItemId(position);
		closeDrawer();
		mainActivity.clearFragmentBackStackImmediate(); // root screen
		StatusLoader.get().clearAllTasks();
		ServiceUpdateLoader.get().clearAllTasks();
		mainActivity.showNewFragment(newFragment, false, true);
		if (this.drawerListViewAdapter.isRootScreen(position)) {
			PreferenceUtils.savePrefLcl(mainActivity, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, this.currentSelectedScreenItemId, false);
		}
	}

	private void showDrawerLoading() {
		this.drawerListView.setVisibility(View.GONE);
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
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
		return this.currentSelectedItemPosition >= 0 && !TextUtils.isEmpty(this.currentSelectedScreenItemId);
	}

	public void setCurrentSelectedItemChecked(boolean value) {
		if (this.drawerListView != null && this.currentSelectedItemPosition >= 0) {
			this.drawerListView.setItemChecked(this.currentSelectedItemPosition, value);
		}
	}

	public void onBackStackChanged(int backStackEntryCount) {
		setCurrentSelectedItemChecked(backStackEntryCount == 0);
	}

	public void onResume() {
	}

	private static final String EXTRA_SELECTED_ROOT_SCREEN_POSITION = "extra_selected_root_screen";
	private static final String EXTRA_SELECTED_ROOT_SCREEN_ID = "extra_selected_root_screen_id";

	public void onSaveState(Bundle outState) {
		if (this.currentSelectedItemPosition >= 0) {
			outState.putInt(EXTRA_SELECTED_ROOT_SCREEN_POSITION, this.currentSelectedItemPosition);
		}
		if (!TextUtils.isEmpty(this.currentSelectedScreenItemId)) {
			outState.putString(EXTRA_SELECTED_ROOT_SCREEN_ID, this.currentSelectedScreenItemId);
		}
	}

	private void showDrawerLoaded() {
		MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
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
		this.currentSelectedItemPosition = -1;
		this.currentSelectedScreenItemId = null;
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
			super(mainActivity, drawerLayout, R.string.drawer_open, R.string.drawer_close);
			this.mainActivityWR = new WeakReference<MainActivity>(mainActivity);
		}

		@Override
		public void onDrawerClosed(View view) {
			MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				ActionBarController abController = mainActivity.getAbController();
				if (abController != null) {
					abController.updateABDrawerClosed();
				}
			}
		}

	}

}
