package org.mtransit.android.ui;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

import java.util.WeakHashMap;

public abstract class MTActivity extends MTAppCompatActivity implements IActivity, MTLog.Loggable {

	@NonNull
	private final WeakHashMap<Fragment, Object> fragmentsWR = new WeakHashMap<>();

	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		super.onAttachFragment(fragment);
		fragmentsWR.put(fragment, null);
	}

	@Nullable
	public java.util.Set<Fragment> getFragments() {
		return fragmentsWR.keySet();
	}

	@NonNull
	@Override
	public Context getContext() {
		return this;
	}

	@NonNull
	@Override
	public Context requireContext() throws IllegalStateException {
		return this;
	}

	@NonNull
	@Override
	public Activity getActivity() {
		return this;
	}

	@NonNull
	@Override
	public Activity requireActivity() throws IllegalStateException {
		return this;
	}

	@NonNull
	@Override
	public LifecycleOwner getLifecycleOwner() {
		return this;
	}
}
