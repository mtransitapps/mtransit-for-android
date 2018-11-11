package org.mtransit.android.common;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface IContext {

	@Nullable
	Context getContext();

	@NonNull
	Context requireContext() throws IllegalStateException;
}
