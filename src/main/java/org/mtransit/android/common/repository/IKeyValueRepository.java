package org.mtransit.android.common.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IKeyValueRepository {

	boolean hasKey(@NonNull String key);

	// Boolean

	boolean getValue(@NonNull String key, boolean defaultValue);

	void saveAsync(@NonNull String key, @Nullable Boolean value);

	// String

	@Nullable
	String getValue(@NonNull String key, @Nullable String defaultValue);

	@NonNull
	String getValueNN(@NonNull String key, @NonNull String defaultValue);

	void saveAsync(@NonNull String key, @Nullable String value);

	// Integer

	int getValue(@NonNull String key, int defaultValue);

	void saveAsync(@NonNull String key, int value);

	// Long

	long getValue(@NonNull String key, long defaultValue);

	void saveAsync(@NonNull String key, long value);

}