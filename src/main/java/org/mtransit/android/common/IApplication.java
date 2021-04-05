package org.mtransit.android.common;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IApplication extends IContext {

	@Nullable
	Application getApplication();

	@NonNull
	Application requireApplication() throws IllegalStateException;
}
