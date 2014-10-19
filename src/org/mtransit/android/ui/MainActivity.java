package org.mtransit.android.ui;

import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.data.DataSourceProvider;
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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

@SuppressWarnings("deprecation")
// need to switch to support-v7-appcompat
public class MainActivity extends MTActivityWithLocation implements AdapterView.OnItemClickListener, FragmentManager.OnBackStackChangedListener {

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

	private Integer mBgColor;
	private Integer mDrawerBgColor;

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
		mBgColor = mDrawerBgColor = ABFragment.NO_BG_COLOR;
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

		getSupportFragmentManager().addOnBackStackChangedListener(this);

		if (savedInstanceState == null) {
			final String itemId = PreferenceUtils.getPrefLcl(this, PreferenceUtils.PREFS_LCL_ROOT_SCREEN_ITEM_ID, MenuAdapter.ITEM_ID_SELECTED_SCREEN_DEFAULT);
			selectItem(this.mDrawerListAdapter.getScreenItemPosition(itemId));
		} else {
			onRestoreState(savedInstanceState);
		}
	}

	private void onRestoreState(Bundle savedInstanceState) {
		int savedRootScreen = savedInstanceState.getInt(EXTRA_SELECTED_ROOT_SCREEN, -1);
		if (savedRootScreen >= 0) {
			this.currentSelectedItemPosition = savedRootScreen;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		DataSourceProvider.reset();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(EXTRA_SELECTED_ROOT_SCREEN, this.currentSelectedItemPosition);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		onRestoreState(savedInstanceState);
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
			while (getSupportFragmentManager().getBackStackEntryCount() > 0) {
				getSupportFragmentManager().popBackStackImmediate();
			}
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
		clearFragmentBackStack(); // root screen
		setAB(newFragment);
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

	private void clearFragmentBackStack() {
		final FragmentManager fm = getSupportFragmentManager();
		for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
			fm.popBackStack();
		}
	}

	public void addFragmentToStack(ABFragment newFragment) {
		setAB(newFragment);
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.content_frame, newFragment);
		ft.addToBackStack(null);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
		mDrawerList.setItemChecked(this.currentSelectedItemPosition, false);
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		final List<Fragment> fragments = getSupportFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof MTActivityWithLocation.UserLocationListener) {
					((MTActivityWithLocation.UserLocationListener) fragment).onUserLocationChanged(newLocation);
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

	private void setAB(ABFragment fragment) {
		setAB(fragment.getABTitle(this), fragment.getSubtitle(this), fragment.getABIconDrawableResId(), fragment.getBgColor());
	}

	private void setAB(CharSequence title, CharSequence subtitle, int iconResId, Integer bgColor) {
		mTitle = title;
		mSubtitle = subtitle;
		mIcon = iconResId;
		mBgColor = bgColor;
		updateAB();
	}
	private void setABTitle(CharSequence title) {
		mTitle = title;
		updateAB();
	}

	@SuppressWarnings("unused")
	private void setABSubtitle(CharSequence subtitle) {
		mSubtitle = subtitle;
		updateAB();
	}

	@SuppressWarnings("unused")
	private void setABIcon(int resId) {
		mIcon = resId;
		updateAB();
	}

	public void notifyABChange() {
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.content_frame);
		if (f != null && f instanceof ABFragment) {
			ABFragment abf = (ABFragment) f;
			mTitle = abf.getABTitle(this);
			mSubtitle = abf.getSubtitle(this);
			mIcon = abf.getABIconDrawableResId();
			mBgColor = abf.getBgColor();
			updateAB();
		}
	}

	@Override
	public void onBackStackChanged() {
		updateAB(); // up/drawer icon
		if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
			mDrawerList.setItemChecked(this.currentSelectedItemPosition, true);
		} else {
			mDrawerList.setItemChecked(this.currentSelectedItemPosition, false);
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

	private void updateAB() {
		if (isDrawerOpen()) {
			updateABDrawerOpened();
		} else {
			updateABDrawerClosed();
		}
	}

	private void updateABDrawerClosed() {
		getActionBar().setTitle(mTitle);
		getActionBar().setSubtitle(mSubtitle);
		if (mIcon > 0) {
			getActionBar().setIcon(mIcon);
		} else {
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
		}
		if (mBgColor != null) {
			getActionBar().setBackgroundDrawable(new ColorDrawable(mBgColor));
		} else {
			getActionBar().setBackgroundDrawable(null);
		}
		this.mDrawerToggle.setDrawerIndicatorEnabled(getSupportFragmentManager().getBackStackEntryCount() < 1);
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
		if (mDrawerBgColor != null) {
			getActionBar().setBackgroundDrawable(new ColorDrawable(mDrawerBgColor));
		} else {
			getActionBar().setBackgroundDrawable(null);
		}
		this.mDrawerToggle.setDrawerIndicatorEnabled(true);
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
		if (item.getItemId() == android.R.id.home && getSupportFragmentManager().getBackStackEntryCount() > 0) {
			getSupportFragmentManager().popBackStack();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
