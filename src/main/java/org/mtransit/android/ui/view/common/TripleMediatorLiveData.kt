package org.mtransit.android.ui.view.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class TripleMediatorLiveData<A, B, C>(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>) :
    MediatorLiveData<Triple<A?, B?, C?>>() {
    init {
        addSource(a) { value = Triple(it, b.value, c.value) }
        addSource(b) { value = Triple(a.value, it, c.value) }
        addSource(c) { value = Triple(a.value, b.value, it) }
    }
}