package org.mtransit.android.common;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IContext {

	@Nullable
	Context getContext();

	@NonNull
	Context requireContext() throws IllegalStateException;
}
