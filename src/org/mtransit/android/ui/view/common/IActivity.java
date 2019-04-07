package org.mtransit.android.ui.view.common;

import org.mtransit.android.common.IContext;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

public interface IActivity extends IContext {

	@Nullable
	Activity getActivity();

	@NonNull
	Activity requireActivity() throws IllegalStateException;

	void finish();

	@Nullable
	<T extends View> T findViewById(@IdRes int id);
}
