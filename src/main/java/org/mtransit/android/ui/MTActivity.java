package org.mtransit.android.ui;

import java.util.WeakHashMap;

import org.mtransit.android.commons.MTLog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class MTActivity extends MTAppCompatActivity implements MTLog.Loggable {

	private WeakHashMap<Fragment, Object> fragmentsWR = new WeakHashMap<>();

	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		super.onAttachFragment(fragment);
		fragmentsWR.put(fragment, null);
	}

	@Nullable
	public java.util.Set<Fragment> getFragments() {
		return fragmentsWR.keySet();
	}
}
