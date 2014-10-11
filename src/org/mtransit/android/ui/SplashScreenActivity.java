package org.mtransit.android.ui;

import org.mtransit.android.commons.ui.MTFragmentActivity;

import android.os.Bundle;

public class SplashScreenActivity extends MTFragmentActivity {

	private static final String TAG = SplashScreenActivity.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		showHomeActivity();
	}

	private void showHomeActivity() {
		startActivity(MainActivity.newInstance(this, -1));
		finish();
	}
}
