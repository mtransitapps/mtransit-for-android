package org.mtransit.android.ui.fragment;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class FavoritesFragment extends ABFragment {

	private static final String TAG = FavoritesFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static FavoritesFragment newInstance() {
		return new FavoritesFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return new FrameLayout(getActivity());
	}

	@Override
	public int getIconDrawableResId() {
		return R.drawable.ic_menu_favorites;
	}

	@Override
	public CharSequence getTitle(Context context) {
		return context.getString(R.string.favorites);
	}

	@Override
	public CharSequence getSubtitle(Context context) {
		return null;
	}
}
