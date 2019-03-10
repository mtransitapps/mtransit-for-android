package org.mtransit.android.common.repository;

import org.mtransit.android.common.IApplication;

import android.content.Context;
import android.support.annotation.NonNull;

public abstract class PreferenceRepository implements IKeyValueRepository {

	@NonNull
	private final IApplication appContext;

	public PreferenceRepository(@NonNull IApplication appContext) {
		this.appContext = appContext;
	}

	@NonNull
	protected Context requireContext() {
		return this.appContext.requireContext();
	}
}
