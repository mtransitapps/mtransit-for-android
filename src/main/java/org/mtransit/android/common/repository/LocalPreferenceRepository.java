package org.mtransit.android.common.repository;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.PreferenceUtils;

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

	@Nullable
	@Override
	public String getValue(@NonNull String key, @Nullable String defaultValue) {
		return PreferenceUtils.getPrefLcl(requireContext(), key, defaultValue);
	}

	@NonNull
	@Override
	public String getValueNN(@NonNull String key, @NonNull String defaultValue) {
		return PreferenceUtils.getPrefLclNN(requireContext(), key, defaultValue);
	}

	@Override
	public void saveAsync(@NonNull String key, @Nullable String value) {
		PreferenceUtils.savePrefLcl(requireContext(), key, value, false);
	}

	@NonNull
	public SharedPreferences getPref() {
		return PreferenceUtils.getPrefLcl(requireContext());
	}
}