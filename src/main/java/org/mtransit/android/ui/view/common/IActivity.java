package org.mtransit.android.ui.view.common;

import android.app.Activity;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import org.mtransit.android.common.IContext;

public interface IActivity extends IContext {

	@Nullable
	Activity getActivity();

	@NonNull
	Activity requireActivity() throws IllegalStateException;

	@NonNull
	LifecycleOwner getLifecycleOwner();

	void finish();

	@Nullable
	<T extends View> T findViewById(@IdRes int id);
}
