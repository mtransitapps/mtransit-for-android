package org.mtransit.android.ui.fragment;

import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.util.AnalyticsUtils;

import android.content.Context;
import android.view.View;

public abstract class ABFragment extends MTFragmentV4 implements AnalyticsUtils.Trackable {

	public static final int NO_ICON = -1;

	public static final Integer NO_BG_COLOR = null;

	public static final View NO_CUSTOM_VIEW = null;

	public abstract CharSequence getABTitle(Context context);

	public CharSequence getABSubtitle(Context context) {
		return null;
	}

	public int getABIconDrawableResId() {
		return ABFragment.NO_ICON;
	}

	public Integer getABBgColor() {
		return ABFragment.NO_BG_COLOR;
	}

	public View getABCustomView() {
		return ABFragment.NO_CUSTOM_VIEW;
	}

	@Override
	public void onResume() {
		super.onResume();
		AnalyticsUtils.trackScreenView(getActivity(), this);
	}

}
