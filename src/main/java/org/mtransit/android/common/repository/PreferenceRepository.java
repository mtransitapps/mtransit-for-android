package org.mtransit.android.common.repository;

import android.content.Context;

import androidx.annotation.NonNull;

public abstract class PreferenceRepository implements IKeyValueRepository {

	@NonNull
	private final Context appContext;

	PreferenceRepository(@NonNull Context appContext) {
		this.appContext = appContext;
	}

	@NonNull
	protected Context requireContext() {
		return this.appContext;
	}
}