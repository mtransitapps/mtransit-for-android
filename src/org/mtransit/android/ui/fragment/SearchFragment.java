package org.mtransit.android.ui.fragment;

import org.mtransit.android.R;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class SearchFragment extends ABFragment {

	private static final String TAG = SearchFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static SearchFragment newInstance() {
		return new SearchFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return new FrameLayout(getActivity());
	}

	@Override
	public int getABIconDrawableResId() {
		return R.drawable.ic_menu_search;
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.search);
	}

	@Override
	public CharSequence getSubtitle(Context context) {
		return null;
	}

	@Override
	public Integer getBgColor() {
		return ABFragment.NO_BG_COLOR;
	}
}
