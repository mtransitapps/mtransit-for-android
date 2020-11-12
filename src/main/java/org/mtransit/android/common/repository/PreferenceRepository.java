package org.mtransit.android.common.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import org.mtransit.android.common.IApplication;

public abstract class PreferenceRepository implements IKeyValueRepository {

	@NonNull
	private final IApplication appContext;

	PreferenceRepository(@NonNull IApplication appContext) {
		this.appContext = appContext;
	}

	@NonNull
	protected Context requireContext() {
		return this.appContext.requireContext();
	}
}