package org.mtransit.android.ui.fragment;

import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.util.AnalyticsUtils;

public abstract class TrackableFragment extends MTFragmentV4 implements AnalyticsUtils.Trackable {

	@Override
	public void onResume() {
		super.onResume();
		AnalyticsUtils.trackScreenView(getActivity(), this);
	}

}
