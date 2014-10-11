package org.mtransit.android.ui;

import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.data.MenuAdapter;
import org.mtransit.android.ui.fragment.ABFragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class MainActivity extends MTActivityWithLocation implements AdapterView.OnItemClickListener {

	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final boolean LOCATION_ENABLED = true;

	private static final String EXTRA_SELECTED_ROOT_SCREEN = "extra_selected_root_screen";

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private MenuAdapter mDrawerListAdapter;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private CharSequence mDrawerSubtitle;
	private CharSequence mSubtitle;

	private int mIcon;
	private int mDrawerIcon;

	public static Intent newInstance(Context context, int selectedRootScreenPosition) {
		Intent intent = new Intent(context, MainActivity.class);
		if (selectedRootScreenPosition >= 0) {
			intent.putExtra(EXTRA_SELECTED_ROOT_SCREEN, selectedRootScreenPosition);
		}
		return intent;
	}

	public MainActivity() {
		super(LOCATION_ENABLED);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTitle = mDrawerTitle = getTitle();
		mSubtitle = mDrawerSubtitle = getActionBar().getSubtitle();
		mIcon = mDrawerIcon = R.drawable.ic_launcher;
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer); // (mDrawerList) getSupportFragmentManager().findFragmentById(R.id.left_drawer);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerListAdapter = new MenuAdapter(this);
		mDrawerList.setAdapter(mDrawerListAdapter);
		mDrawerList.setOnItemClickListener(this);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
			@Override
			public void onDrawerClosed(View view) {
				updateABDrawerClosed();
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				updateABDrawerOpened();
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			final String itemId = PreferenceUtils.getPrefLcl(this, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, MenuAdapter.ITEM_ID_SELECTED_SCREEN_DEFAULT);
			selectItem(this.mDrawerListAdapter.getScreenItemPosition(itemId));
		} else {
			int savedRootScreen = savedInstanceState.getInt(EXTRA_SELECTED_ROOT_SCREEN, -1);
			if (savedRootScreen >= 0) {
				selectItem(savedRootScreen);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(EXTRA_SELECTED_ROOT_SCREEN, this.currentSelectedItemPosition);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int savedRootScreen = savedInstanceState.getInt(EXTRA_SELECTED_ROOT_SCREEN, -1);
		if (savedRootScreen >= 0) {
			selectItem(savedRootScreen);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		selectItem(position);
	}


	private int currentSelectedItemPosition = -1;

	private void selectItem(int position) {
		if (position < 0) {
			return;
		}
		if (position == this.currentSelectedItemPosition) {
			closeDrawer();
			return;
		}
		if (!this.mDrawerListAdapter.isRootScreen(position)) {
			if (this.currentSelectedItemPosition >= 0) {
				mDrawerList.setItemChecked(this.currentSelectedItemPosition, true); // keep current position
			}
			return;
		}
		final ABFragment newFragment = this.mDrawerListAdapter.getNewStaticFragmentAt(position);
		if (newFragment == null) {
			return;
		}
		setABTitle(newFragment.getTitle(this));
		setABSubtitle(newFragment.getSubtitle(this));
		setABIcon(newFragment.getIconDrawableResId());
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.content_frame, newFragment);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
		mDrawerList.setItemChecked(position, true);
		closeDrawer();
		this.currentSelectedItemPosition = position;
		if (this.mDrawerListAdapter.isRootScreen(position)) {
			PreferenceUtils.savePrefLcl(this, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, this.mDrawerListAdapter.getScreenItemId(position), false);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		super.onLocationChanged(location);
		// ALL FRAGMENTs
		List<Fragment> fragments = getSupportFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof MTActivityWithLocation.UserLocationListener) {
					((MTActivityWithLocation.UserLocationListener) fragment).onUserLocationChanged(location);
				}
			}
		}
	}

	@Deprecated
	@Override
	public void setTitle(int titleId) {
		super.setTitle(titleId); // call setTitle(CharSequence)
	}

	@Override
	@Deprecated
	public void setTitle(CharSequence title) {
		setABTitle(title);
	}

	private void setABTitle(CharSequence title) {
		mTitle = title;
		updateAB();
	}

	private void setABSubtitle(CharSequence subtitle) {
		mSubtitle = subtitle;
		updateAB();
	}

	private void setABIcon(int resId) {
		mIcon = resId;
		updateAB();
	}

	public void notifyABChange() {
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.content_frame);
		if (f != null && f instanceof ABFragment) {
			ABFragment abf = (ABFragment) f;
			mTitle = abf.getTitle(this);
			mSubtitle = abf.getSubtitle(this);
			mIcon = abf.getIconDrawableResId();
			updateAB();
		}
	}

	private void updateAB() {
		if (isDrawerOpen()) {
			updateABDrawerOpened();
		} else {
			updateABDrawerClosed();
		}
	}

	@Override
	public void onBackPressed() {
		if (isDrawerOpen()) {
			closeDrawer();
			return;
		}
		super.onBackPressed();
	}

	private void closeDrawer() {
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	private boolean isDrawerOpen() {
		return mDrawerLayout.isDrawerOpen(mDrawerList);
	}

	private void updateABDrawerClosed() {
		getActionBar().setTitle(mTitle);
		getActionBar().setSubtitle(mSubtitle);
		if (mIcon > 0) {
			getActionBar().setIcon(mIcon);
		} else {
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
		}
		invalidateOptionsMenu();
	}

	private void updateABDrawerOpened() {
		getActionBar().setTitle(mDrawerTitle);
		getActionBar().setSubtitle(mDrawerSubtitle);
		if (mDrawerIcon > 0) {
			getActionBar().setIcon(mDrawerIcon);
		} else {
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
		}
		invalidateOptionsMenu();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
