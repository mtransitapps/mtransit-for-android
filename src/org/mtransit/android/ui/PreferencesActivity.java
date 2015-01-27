package org.mtransit.android.ui;

import org.mtransit.android.R;
import org.mtransit.android.commons.ui.MTActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

public class PreferencesActivity extends MTActivity {

	private static final String TAG = PreferencesActivity.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static Intent newInstance(Context context) {
		return new Intent(context, PreferencesActivity.class);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preferences);
		getActionBar().setTitle(R.string.settings);
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
