package org.mtransit.android.ui.view.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged

fun <T> SavedStateHandle.getLiveDataDistinct(key: String): LiveData<T> {
    return this.getLiveData<T>(key).distinctUntilChanged()
}

fun <T> SavedStateHandle.getLiveDataDistinct(key: String, initialValue: T? = null): LiveData<T> {
    return this.getLiveData<T>(key, initialValue).distinctUntilChanged()
}