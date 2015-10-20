package org.mtransit.android.ui.fragment;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTPreferenceFragment extends PreferenceFragment implements MTLog.Loggable {

	@Override
	public void addPreferencesFromResource(int preferencesResId) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "addPreferencesFromResource(%s)", preferencesResId);
		}
		super.addPreferencesFromResource(preferencesResId);
	}

	@Override
	public Preference findPreference(CharSequence key) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "findPreference(%s)", key);
		}
		return super.findPreference(key);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPreferenceTreeClick(%s,%s)", preferenceScreen, preference);
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "setPreferenceScreen(%s)", preferenceScreen);
		}
		super.setPreferenceScreen(preferenceScreen);
	}

	// INHERITED FROM FRAGMENT

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onActivityCreated(%s)", savedInstanceState);
		}
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onActivityResult(%s,%s,%s)", requestCode, resultCode, data);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onAttach(%s)", activity);
		}
		super.onAttach(activity);
	}

	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public void onAttach(Context context) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onAttach(%s)", context);
		}
		super.onAttach(context);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onConfigurationChanged(%s)", newConfig);
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreate(%s)", savedInstanceState);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateView(%s,%s,%s)", inflater, container, savedInstanceState);
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onDestroy() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDestroy()");
		}
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDestroyView()");
		}
		super.onDestroyView();
	}

	@Override
	public void onDetach() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDetach()");
		}
		super.onDetach();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onInflate(%s,%s,%s)", activity, attrs, savedInstanceState);
		}
		super.onInflate(activity, attrs, savedInstanceState);
	}

	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onInflate(%s,%s,%s)", context, attrs, savedInstanceState);
		}
		super.onInflate(context, attrs, savedInstanceState);
	}

	@Override
	public void onLowMemory() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onLowMemory()");
		}
		super.onLowMemory();
	}

	@Override
	public void onPause() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPause()");
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onResume()");
		}
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onSaveInstanceState(%s)", outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStart()");
		}
		super.onStart();
	}

	@Override
	public void onStop() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStop()");
		}
		super.onStop();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onViewCreated(%s, %s)", view, savedInstanceState);
		}
		super.onViewCreated(view, savedInstanceState);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onViewStateRestored(%s)", savedInstanceState);
		}
		super.onViewStateRestored(savedInstanceState);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateContextMenu(%s,%s,%s)", menu, v, menuInfo);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onContextItemSelected(%s)", item);
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateOptionsMenu(%s,%s)", menu, inflater);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	/**
	 * @deprecated menu items are always visible in the action bar so {@link #onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)} is always
	 *             called before anyway
	 */
	@Deprecated
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPrepareOptionsMenu(%s)", menu);
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onOptionsItemSelected(%s)", item);
		}
		return super.onOptionsItemSelected(item);
	}

}
