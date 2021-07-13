package org.mtransit.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.util.NightModeUtils;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;

@AndroidEntryPoint
public class PreferencesActivity extends MTActivity {

	private static final String LOG_TAG = PreferencesActivity.class.getSimpleName();

	private static final String EXTRA_SUPPORT = "extra_support";

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private boolean showSupport = false;

	private int currentUiMode = -1;

	@NonNull
	public static Intent newInstance(@NonNull Context context) {
		return newInstance(context, false);
	}

	@NonNull
	public static Intent newInstance(@NonNull Context context, boolean support) {
		Intent intent = new Intent(context, PreferencesActivity.class);
		intent.putExtra(EXTRA_SUPPORT, support);
		return intent;
	}

	@EntryPoint
	@InstallIn(SingletonComponent.class)
	interface MTApplicationEntryPoint {
		DemoModeManager demoModeManager(); // used in attachBaseContext() before @Inject dependencies are available
	}

	@NonNull
	private MTApplicationEntryPoint getEntryPoint(@NonNull Context context) {
		return EntryPointAccessors.fromApplication(context.getApplicationContext(), MTApplicationEntryPoint.class);
	}

	@Override
	protected void attachBaseContext(@NonNull Context newBase) {
		final DemoModeManager demoModeManager = getEntryPoint(newBase).demoModeManager();
		if (demoModeManager.getEnabled()) {
			newBase = demoModeManager.fixLocale(newBase);
		} else {
			newBase = LocaleUtils.fixDefaultLocale(newBase);
		}
		super.attachBaseContext(newBase);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.currentUiMode = getResources().getConfiguration().uiMode;
		setContentView(R.layout.activity_preferences);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.settings);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		restoreInstanceState(savedInstanceState, getIntent().getExtras());
	}

	private void restoreInstanceState(Bundle... bundles) {
		Boolean newShowSupport = BundleUtils.getBoolean(EXTRA_SUPPORT, bundles);
		if (newShowSupport != null) {
			this.showSupport = newShowSupport;
		}
	}

	public boolean isShowSupport() {
		return showSupport;
	}

	public void setShowSupport(boolean showSupport) {
		this.showSupport = showSupport;
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(EXTRA_SUPPORT, this.showSupport);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (this.currentUiMode != newConfig.uiMode) {
			NightModeUtils.resetColorCache();
			NightModeUtils.recreate(this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}
}
