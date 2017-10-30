package org.mtransit.android.ui;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.util.VendingUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

public class PreferencesActivity extends MTActivity {

	private static final String TAG = PreferencesActivity.class.getSimpleName();

	private static final String EXTRA_SUPPORT = "extra_support";

	@Override
	public String getLogTag() {
		return TAG;
	}

	private boolean showSupport = false;
	public static Intent newInstance(Context context) {
		return newInstance(context, false);
	}

	public static Intent newInstance(Context context, boolean support) {
		Intent intent = new Intent(context, PreferencesActivity.class);
		intent.putExtra(EXTRA_SUPPORT, support);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(EXTRA_SUPPORT, this.showSupport);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!VendingUtils.onActivityResult(this, requestCode, resultCode, data)) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}
}
