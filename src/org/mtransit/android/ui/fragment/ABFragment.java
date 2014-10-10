package org.mtransit.android.ui.fragment;

import org.mtransit.android.commons.ui.fragment.MTFragmentV4;

import android.content.Context;

public abstract class ABFragment extends MTFragmentV4 {

	public abstract CharSequence getTitle(Context context);

	public abstract CharSequence getSubtitle(Context context);

	public abstract int getIconDrawableResId();

}
