package org.mtransit.android.common;

import android.app.Application;
import android.support.annotation.NonNull;

public interface IApplication extends IContext {

	@NonNull
	Application getApplication();
}
