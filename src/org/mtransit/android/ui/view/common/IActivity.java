package org.mtransit.android.ui.view.common;

import org.mtransit.android.common.IContext;

import android.app.Activity;
import android.support.annotation.NonNull;

public interface IActivity extends IContext {

	@NonNull
	Activity getActivity();

	void finish();
}
