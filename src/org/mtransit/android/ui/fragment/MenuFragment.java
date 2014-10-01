package org.mtransit.android.ui.fragment;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MenuFragment extends MTFragmentV4 {

	public static final String TAG = MenuFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static MenuFragment newInstance() {
		MenuFragment f = new MenuFragment();
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_menu, container, false);
	}
}
