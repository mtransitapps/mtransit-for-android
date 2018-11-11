package org.mtransit.android.common;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface IApplication extends IContext {

	@Nullable
	Application getApplication();

	@NonNull
	Application requireApplication() throws IllegalStateException;
}
