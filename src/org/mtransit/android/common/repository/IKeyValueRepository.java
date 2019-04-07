package org.mtransit.android.common.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface IKeyValueRepository {

	boolean hasKey(@NonNull String key);

	boolean getValue(@NonNull String key, boolean defaultValue);

	void saveAsync(@NonNull String key, @Nullable Boolean value);
}
