package org.mtransit.android.ui.view.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.commons.model.Quadruple

class QuadrupleMediatorLiveData<A, B, C, D>(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>, d: LiveData<D>) :
    MediatorLiveData<Quadruple<A?, B?, C?, D?>>(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = QuadrupleMediatorLiveData::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    init {
        addSource(a) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for A '$it'")
            }
            value = Quadruple(it, b.value, c.value, d.value)
        }
        addSource(b) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for B '$it'")
            }
            value = Quadruple(a.value, it, c.value, d.value)
        }
        addSource(c) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for C '$it'")
            }
            value = Quadruple(a.value, b.value, it, d.value)
        }
        addSource(d) {
            if (Constants.LOG_LIVE_DATA_MEDIATOR) {
                MTLog.d(this, "${hashCode()}: new value for D '$it'")
            }
            value = Quadruple(a.value, b.value, c.value, it)
        }
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(${this.value?.toString()})"
    }
}