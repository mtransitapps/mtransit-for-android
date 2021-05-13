package org.mtransit.android.ui.view.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.mtransit.commons.model.Quintuple

class QuintupleMediatorLiveData<A, B, C, D, E>(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>, d: LiveData<D>, e: LiveData<E>) :
    MediatorLiveData<Quintuple<A?, B?, C?, D?, E?>>() {
    init {
        addSource(a) { value = Quintuple(it, b.value, c.value, d.value, e.value) }
        addSource(b) { value = Quintuple(a.value, it, c.value, d.value, e.value) }
        addSource(c) { value = Quintuple(a.value, b.value, it, d.value, e.value) }
        addSource(d) { value = Quintuple(a.value, b.value, c.value, it, e.value) }
        addSource(e) { value = Quintuple(a.value, b.value, c.value, d.value, it) }
    }
}