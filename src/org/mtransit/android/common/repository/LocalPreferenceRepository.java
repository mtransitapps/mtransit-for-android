package org.mtransit.android.common.repository;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.PreferenceUtils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class LocalPreferenceRepository extends PreferenceRepository {

	public LocalPreferenceRepository(@NonNull IApplication appContext) {
		super(appContext);
	}

	@Override
	public boolean hasKey(@NonNull String key) {
		return PreferenceUtils.hasPrefLcl(requireContext(), key);
	}

	@Override
	public boolean getValue(@NonNull String key, boolean defaultValue) {
		return PreferenceUtils.getPrefLcl(requireContext(), key, defaultValue);
	}

	@Override
	public void saveAsync(@NonNull String key, @Nullable Boolean value) {
		PreferenceUtils.savePrefLcl(requireContext(), key, value, false);
	}
}
