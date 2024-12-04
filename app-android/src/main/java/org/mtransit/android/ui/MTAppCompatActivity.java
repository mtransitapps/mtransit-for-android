package org.mtransit.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.CallSuper;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

/**
 * NO LOGIC HERE, just logs.
 */
@SuppressWarnings("WeakerAccess")
public abstract class MTAppCompatActivity extends AppCompatActivity implements MTLog.Loggable {

	public MTAppCompatActivity() {
		super();
	}

	@ContentView
	public MTAppCompatActivity(@LayoutRes int contentLayoutId) {
		super(contentLayoutId);
	}

	@Override
	protected void attachBaseContext(@NonNull Context newBase) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "attachBaseContext(%s)", newBase);
		}
		super.attachBaseContext(newBase);
	}

	@Override
	public void applyOverrideConfiguration(@NonNull Configuration overrideConfiguration) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "applyOverrideConfiguration(%s)", overrideConfiguration);
		}
		super.applyOverrideConfiguration(overrideConfiguration);
	}

	// INHERITED FROM FRAGMENT ACTIVITY

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreate(%s)", savedInstanceState);
		}
		super.onCreate(savedInstanceState);
	}

	@CallSuper
	@Override
	protected void onNewIntent(@SuppressWarnings("InvalidNullabilityOverride") Intent intent) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onNewIntent(%s)", intent);
		}
		super.onNewIntent(intent);
	}

	@Override
	protected void onRestart() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onRestart()");
		}
		super.onRestart();
	}

	@Override
	protected void onStart() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStart()");
		}
		super.onStart();
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onRestoreInstanceState(%s)", savedInstanceState);
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onResume()");
		}
		super.onResume();
	}

	@Override
	protected void onResumeFragments() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onResumeFragments()");
		}
		super.onResumeFragments();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onWindowFocusChanged(%s)", hasFocus);
		}
		super.onWindowFocusChanged(hasFocus);
	}

	@Override
	protected void onPostResume() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPostResume()");
		}
		super.onPostResume();
	}

	@Override
	protected void onPause() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPause()");
		}
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onSaveInstanceState(%s)", outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStop()");
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDestroy()");
		}
		super.onDestroy();
	}

	@Override
	public void invalidateOptionsMenu() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "invalidateOptionsMenu()");
		}
		super.invalidateOptionsMenu();
	}

	@Override
	public void supportInvalidateOptionsMenu() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "supportInvalidateOptionsMenu()");
		}
		super.supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateOptionsMenu(%s)", menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * @deprecated menu items are always visible in the action bar so {@link #onCreateOptionsMenu(Menu)} is always called before anyway
	 */
	@Deprecated
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPrepareOptionsMenu(%s)", menu);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onOptionsItemSelected(%s)", item);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onSearchRequested() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onSearchRequested()");
		}
		return super.onSearchRequested();
	}

	@Override
	public void startSearch(@Nullable String initialQuery, boolean selectInitialQuery, @Nullable Bundle appSearchData, boolean globalSearch) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "startSearch(%s,%s,%s,%s)", initialQuery, selectInitialQuery, appSearchData, globalSearch);
		}
		super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPostCreate(%s)", savedInstanceState);
		}
		super.onPostCreate(savedInstanceState);
	}

	@Override
	public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPostCreate(%s,%s)", savedInstanceState, persistentState);
		}
		super.onPostCreate(savedInstanceState, persistentState);
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onConfigurationChanged(%s)", newConfig);
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void recreate() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "recreate()");
		}
		super.recreate();
	}
}
