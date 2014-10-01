package org.mtransit.android.ui;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.MenuFragment;
import org.mtransit.android.ui.fragment.NearbyFragment;

import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class MainActivity extends MTActivityWithLocation {

	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final boolean LOCATION_ENABLED = true;

	private DrawerLayout mDrawerLayout;
	private MenuFragment mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private CharSequence mDrawerSubtitle;
	private CharSequence mSubtitle;

	public MainActivity() {
		super(LOCATION_ENABLED);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTitle = mDrawerTitle = getTitle();
		mSubtitle = mDrawerSubtitle = getActionBar().getSubtitle();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (MenuFragment) getSupportFragmentManager().findFragmentById(R.id.left_drawer);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
			@Override
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				getActionBar().setSubtitle(mSubtitle);
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				mTitle = getActionBar().getTitle();
				mSubtitle = getActionBar().getSubtitle();
				getActionBar().setTitle(mDrawerTitle);
				getActionBar().setSubtitle(mDrawerSubtitle);
				invalidateOptionsMenu();
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			selectItem(0);
		}
	}

	private void selectItem(int position) {
		Fragment fragment = NearbyFragment.newInstance(null);

		getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment, NearbyFragment.FRAGMENT_TAG).commit();

		setTitle("Nearby"); // FIXME i18n
		setSubtitle(null);
		mDrawerLayout.closeDrawer(mDrawerList.getView());
	}

	@Override
	public void onLocationChanged(Location location) {
		super.onLocationChanged(location);
		// ALL FRAGMENTs
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof MTActivityWithLocation.UserLocationListener) {
				((MTActivityWithLocation.UserLocationListener) fragment).onUserLocationChanged(location);
			}
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	public void setSubtitle(CharSequence subtitle) {
		mSubtitle = subtitle;
		getActionBar().setSubtitle(mSubtitle);
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
	}
}
