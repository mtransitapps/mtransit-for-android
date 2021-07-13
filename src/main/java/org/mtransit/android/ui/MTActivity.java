package org.mtransit.android.ui;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

public abstract class MTActivity extends MTAppCompatActivity implements IActivity, MTLog.Loggable {

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
