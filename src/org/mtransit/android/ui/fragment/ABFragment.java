package org.mtransit.android.ui.fragment;

import org.mtransit.android.commons.ui.fragment.MTFragmentV4;

import android.content.Context;

public abstract class ABFragment extends MTFragmentV4 {

	public static final int NO_ICON = -1;

	public static final Integer NO_BG_COLOR = null;

	public abstract CharSequence getABTitle(Context context);

	public abstract CharSequence getSubtitle(Context context);

	public abstract int getABIconDrawableResId();

	public abstract Integer getBgColor();

}
