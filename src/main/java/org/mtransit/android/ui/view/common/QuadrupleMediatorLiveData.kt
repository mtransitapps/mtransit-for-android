package org.mtransit.android.ui.view.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.mtransit.commons.model.Quadruple

class QuadrupleMediatorLiveData<A, B, C, D>(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>, d: LiveData<D>) :
    MediatorLiveData<Quadruple<A?, B?, C?, D?>>() {
    init {
        addSource(a) { value = Quadruple(it, b.value, c.value, d.value) }
        addSource(b) { value = Quadruple(a.value, it, c.value, d.value) }
        addSource(c) { value = Quadruple(a.value, b.value, it, d.value) }
        addSource(d) { value = Quadruple(a.value, b.value, c.value, it) }
    }
}