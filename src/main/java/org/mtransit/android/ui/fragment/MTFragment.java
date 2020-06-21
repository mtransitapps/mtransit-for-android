package org.mtransit.android.ui.fragment;

import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.MTLog;

import java.util.Set;
import java.util.WeakHashMap;

public abstract class MTFragment extends MTFragmentV4 implements IContext, MTLog.Loggable {

	public MTFragment() {
		super();
	}

	@ContentView
	public MTFragment(@LayoutRes int contentLayoutId) {
		super(contentLayoutId);
	}

	@NonNull
	private final WeakHashMap<Fragment, Object> childFragmentsWR = new WeakHashMap<>();

	@Override
	public void onAttachFragment(@NonNull Fragment childFragment) {
		super.onAttachFragment(childFragment);
		childFragmentsWR.put(childFragment, null);
	}

	@Nullable
	Set<Fragment> getChildFragments() {
		return childFragmentsWR.keySet();
	}
}
