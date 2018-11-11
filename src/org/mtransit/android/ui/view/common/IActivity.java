package org.mtransit.android.ui.view.common;

import org.mtransit.android.common.IContext;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface IActivity extends IContext {

	@Nullable
	Activity getActivity();

	@NonNull
	Activity requireActivity() throws IllegalStateException;

	void finish();
}
