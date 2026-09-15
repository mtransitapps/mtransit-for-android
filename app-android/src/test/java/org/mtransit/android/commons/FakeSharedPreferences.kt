package org.mtransit.android.commons

import android.content.SharedPreferences

class FakeSharedPreferences : SharedPreferences {

    private val storage = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = storage.toMap()

    override fun getString(key: String, defValue: String?): String? =
        storage[key] as? String ?: defValue

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        @Suppress("UNCHECKED_CAST") (storage[key] as? Set<String> ?: defValues)

    override fun getInt(key: String, defValue: Int): Int =
        storage[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        storage[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        storage[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        storage[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = storage.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }

    fun reset() {
        storage.clear()
        listeners.clear()
    }

    private inner class FakeEditor : SharedPreferences.Editor {
        private val tempStorage = mutableMapOf<String, Any?>()
        private val keysToRemove = mutableSetOf<String>()
        private var clearCalled = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            tempStorage[key] = value
            keysToRemove.remove(key)
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = apply {
            tempStorage[key] = values?.toSet()
            keysToRemove.remove(key)
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
            tempStorage[key] = value
            keysToRemove.remove(key)
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
            tempStorage[key] = value
            keysToRemove.remove(key)
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
            tempStorage[key] = value
            keysToRemove.remove(key)
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
            tempStorage[key] = value
            keysToRemove.remove(key)
        }

        override fun remove(key: String): SharedPreferences.Editor = apply {
            keysToRemove.add(key)
            tempStorage.remove(key)
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearCalled = true
            tempStorage.clear()
            keysToRemove.clear()
        }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

        private fun applyChanges() {
            val modifiedKeys = mutableSetOf<String>()

            if (clearCalled) {
                modifiedKeys.addAll(storage.keys)
                storage.clear()
            }

            keysToRemove.forEach { key ->
                if (storage.containsKey(key)) {
                    storage.remove(key)
                    modifiedKeys.add(key)
                }
            }

            tempStorage.forEach { (key, value) ->
                if (storage[key] != value) {
                    storage[key] = value
                    modifiedKeys.add(key)
                }
            }

            modifiedKeys.forEach { key ->
                listeners.forEach { it.onSharedPreferenceChanged(this@FakeSharedPreferences, key) }
            }
        }
    }
}
