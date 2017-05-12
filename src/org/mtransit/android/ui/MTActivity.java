package org.mtransit.android.ui;

import java.util.WeakHashMap;

import org.mtransit.android.commons.MTLog;

import android.support.v4.app.Fragment;

public abstract class MTActivity extends MTAppCompatActivity implements MTLog.Loggable {

	private WeakHashMap<Fragment, Object> fragmentsWR = new WeakHashMap<Fragment, Object>();

	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		fragmentsWR.put(fragment, null);
	}

	public java.util.Set<Fragment> getFragments() {
		return fragmentsWR.keySet();
	}
}
