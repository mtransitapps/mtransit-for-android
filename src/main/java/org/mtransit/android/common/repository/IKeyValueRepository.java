package org.mtransit.android.common.repository;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/** @noinspection unused*/
public interface IKeyValueRepository {

	@WorkerThread
	boolean hasKey(@NonNull String key);

	// Boolean

	@WorkerThread
	boolean getValue(@NonNull String key, boolean defaultValue);

	@MainThread
	void saveAsync(@NonNull String key, @Nullable Boolean value);

	// String

	@WorkerThread
	@Nullable
	String getValue(@NonNull String key, @Nullable String defaultValue);

	@WorkerThread
	@NonNull
	String getValueNN(@NonNull String key, @NonNull String defaultValue);

	@MainThread
	void saveAsync(@NonNull String key, @Nullable String value);

	// Integer

	@WorkerThread
	int getValue(@NonNull String key, int defaultValue);

	@MainThread
	void saveAsync(@NonNull String key, int value);

	// Long

	@WorkerThread
	long getValue(@NonNull String key, long defaultValue);

	@MainThread
	void saveAsync(@NonNull String key, long value);

}