package org.mtransit.android.common.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IKeyValueRepository {

	boolean hasKey(@NonNull String key);

	boolean getValue(@NonNull String key, boolean defaultValue);

	void saveAsync(@NonNull String key, @Nullable Boolean value);

	@Nullable
	String getValue(@NonNull String key, @Nullable String defaultValue);

	@NonNull
	String getValueNN(@NonNull String key, @NonNull String defaultValue);

	void saveAsync(@NonNull String key, @Nullable String value);
}