package org.mtransit.android.common.repository;

public interface IKeyValueRepository {

	boolean hasKey(String key);

	boolean getValue(String key, boolean defaultValue);

	void saveAsync(String key, Boolean value);
}
