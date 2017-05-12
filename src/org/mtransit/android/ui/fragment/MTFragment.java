package org.mtransit.android.ui.fragment;

import java.util.WeakHashMap;

import org.mtransit.android.commons.MTLog;

import android.support.v4.app.Fragment;

public abstract class MTFragment extends MTFragmentV4 implements MTLog.Loggable {

	private WeakHashMap<Fragment, Object> childFragmentsWR = new WeakHashMap<Fragment, Object>();

	@Override
	public void onAttachFragment(Fragment childFragment) {
		super.onAttachFragment(childFragment);
		childFragmentsWR.put(childFragment, null);
	}

	public java.util.Set<Fragment> getChildFragments() {
		return childFragmentsWR.keySet();
	}
}
